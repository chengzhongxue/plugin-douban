package la.moony.douban;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import la.moony.douban.extension.CronDouban;
import la.moony.douban.service.DoubanService;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CronDoubanReconciler  implements Reconciler<Reconciler.Request>{

    private static final Logger log = LoggerFactory.getLogger(CronDoubanReconciler.class);

    private final ExtensionClient client;

    private final DoubanService doubanService;
    private Clock clock;


    public CronDoubanReconciler(ExtensionClient client, DoubanService doubanService) {
        this.client = client;
        this.doubanService = doubanService;
        this.clock = Clock.systemDefaultZone();
    }


    void setClock(Clock clock) {
        this.clock = clock;
    }


    public Reconciler.Result reconcile(Reconciler.Request request) {
        return (Reconciler.Result)this.client.fetch(CronDouban.class, request.name()).map((cronDouban) -> {
            if (ExtensionUtil.isDeleted(cronDouban)) {
                CronDouban createCronDouban = new CronDouban();
                createCronDouban.setSpec(new CronDouban.Spec());
                createCronDouban.setStatus(new CronDouban.Status());
                createCronDouban.getSpec().setCron("@daily");
                createCronDouban.getSpec().setTimezone("Asia/Shanghai");
                createCronDouban.setMetadata(new Metadata());
                createCronDouban.getMetadata().setName("cron-default");
                this.client.create(createCronDouban);
                return Result.doNotRetry();
            } else {
                CronDouban.Spec spec = cronDouban.getSpec();
                String cron = spec.getCron();
                String timezone = spec.getTimezone();
                ZoneId zoneId = ZoneId.systemDefault();
                if (timezone != null) {
                    try {
                        zoneId = (ZoneId) ApplicationConversionService.getSharedInstance().convert(timezone, ZoneId.class);
                    } catch (DateTimeException var18) {
                        log.error("Invalid zone ID {}", timezone, var18);
                        return Result.doNotRetry();
                    }
                }
                Instant now = Instant.now(this.clock);
                if (!CronExpression.isValidExpression(cron)) {
                    log.error("Cron expression {} is invalid.", cron);
                    return Result.doNotRetry();
                } else {
                    CronExpression cronExp = CronExpression.parse(cron);
                    CronDouban.Status status = cronDouban.getStatus();
                    Instant lastScheduledTimestamp = status.getLastScheduledTimestamp();
                    if (lastScheduledTimestamp == null) {
                        lastScheduledTimestamp = cronDouban.getMetadata().getCreationTimestamp();
                    }

                    ZonedDateTime nextFromNow = (ZonedDateTime)cronExp.next(now.atZone(zoneId));
                    ZonedDateTime nextFromLast = (ZonedDateTime)cronExp.next(lastScheduledTimestamp.atZone(zoneId));

                    if (nextFromNow != null && nextFromLast != null) {
                        if (Objects.equals(nextFromNow, nextFromLast)) {
                            log.info("Skip scheduling and next scheduled at {}", nextFromNow);
                            status.setNextSchedulingTimestamp(nextFromNow.toInstant());
                            this.client.update(cronDouban);
                            return new Reconciler.Result(true, Duration.between(now, nextFromNow));
                        } else {

                            this.doubanService.synchronizationDouban();

                            ZonedDateTime zonedNow = now.atZone(zoneId);
                            ZonedDateTime scheduleTimestamp = now.atZone(zoneId);

                            ZonedDateTime next;
                            for(next = lastScheduledTimestamp.atZone(zoneId); next != null && next.isBefore(zonedNow); next = (ZonedDateTime)cronExp.next(next)) {
                                scheduleTimestamp = next;
                            }

                            status.setLastScheduledTimestamp(scheduleTimestamp.toInstant());
                            if (next != null) {
                                status.setNextSchedulingTimestamp(next.toInstant());
                            }

                            this.client.update(cronDouban);
                            log.info("Scheduled at {} and next scheduled at {}", scheduleTimestamp, next);
                            return new Reconciler.Result(true, Duration.between(now, next));
                        }

                    } else {
                        return Result.doNotRetry();
                    }

                }
            }

         }).orElseGet(Reconciler.Result::doNotRetry);
    }


    public Controller setupWith(ControllerBuilder builder) {
        return builder.extension(new CronDouban()).workerCount(1).build();
    }
}

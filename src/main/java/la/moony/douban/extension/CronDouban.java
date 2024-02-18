package la.moony.douban.extension;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;
import java.time.Instant;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@GVK(
    group = "douban.moony.la",
    version = "v1alpha1",
    kind = "CronDouban",
    singular = "crondouban",
    plural = "crondoubans"
)
public class CronDouban extends AbstractExtension {


    private Spec spec = new Spec();

    private Status status = new Status();

    @Data
    public static class Spec {
        private String cron;
        private String timezone;
        private boolean suspend;

    }

    @Data
    public static class Status {
        private Instant lastScheduledTimestamp;
        private Instant nextSchedulingTimestamp;

    }
}

package la.moony.douban.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;
import java.time.Instant;
import java.util.Set;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@GVK(kind = "DoubanMovie", group = "douban.moony.la",
    version = "v1alpha1", singular = "doubanmovie", plural = "doubanmovies")
public class DoubanMovie extends AbstractExtension {

    public static final String REQUIRE_SYNC_ON_STARTUP_INDEX_NAME = "requireSyncOnStartup";


    @Schema(requiredMode = REQUIRED)
    private DoubanMovieSpec spec;

    @Schema(requiredMode = REQUIRED)
    private DoubanMovieFaves faves;

    private Status status;


    @Data
    public static class DoubanMovieSpec {

        private String name;
        private String poster;
        private String link;
        private String id;
        private String score;
        private String year;
        private String type;
        private String pubdate;
        private String cardSubtitle;
        private String dataType;

        private Set<String> genres;

    }

    @Data
    public static class DoubanMovieFaves {
        private String remark;
        private Instant createTime;
        private String score;
        private String status;
    }


    @Data
    @Schema(name = "DoubanStatus")
    public static class Status {
        private long observedVersion;
    }



}

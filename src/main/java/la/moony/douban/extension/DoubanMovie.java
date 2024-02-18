package la.moony.douban.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;
import java.time.Instant;
import java.util.Set;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@GVK(kind = "DoubanMovie", group = "douban.moony.la",
    version = "v1alpha1", singular = "doubanmovie", plural = "doubanmovies")
public class DoubanMovie extends AbstractExtension {


    private DoubanMovieSpec spec;

    private DoubanMovieFaves faves;


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



}

package la.moony.douban.vo;

import la.moony.douban.extension.DoubanMovie;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.util.Assert;
import run.halo.app.extension.MetadataOperator;

@Data
@SuperBuilder
@ToString
@EqualsAndHashCode
public class DoubanMovieVo {

    private MetadataOperator metadata;

    private DoubanMovie.DoubanMovieSpec spec;

    private DoubanMovie.DoubanMovieFaves faves;

    public static DoubanMovieVo from(DoubanMovie doubanMovie) {
        Assert.notNull(doubanMovie, "The doubanMovie must not be null.");
        return DoubanMovieVo.builder()
            .metadata(doubanMovie.getMetadata())
            .spec(doubanMovie.getSpec())
            .faves(doubanMovie.getFaves())
            .build();
    }

}

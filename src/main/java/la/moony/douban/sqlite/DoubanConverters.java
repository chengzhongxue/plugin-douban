package la.moony.douban.sqlite;

import java.util.HashSet;
import la.moony.douban.extension.DoubanMovie;
import la.moony.douban.sqlite.entity.DoubanMovieData;

public final class DoubanConverters {

    private DoubanConverters() {
    }

    public static DoubanMovieData toMovieData(DoubanMovie movie) {
        DoubanMovieData data = new DoubanMovieData();
        if (movie.getMetadata() != null) {
            data.setId(movie.getMetadata().getName());
            data.setCreationTimestamp(movie.getMetadata().getCreationTimestamp());
        }
        DoubanMovie.DoubanMovieSpec spec = movie.getSpec();
        if (spec != null) {
            data.setName(spec.getName());
            data.setPoster(spec.getPoster());
            data.setLink(spec.getLink());
            data.setDoubanId(spec.getId());
            data.setScore(spec.getScore());
            data.setYear(spec.getYear());
            data.setType(spec.getType());
            data.setPubdate(spec.getPubdate());
            data.setCardSubtitle(spec.getCardSubtitle());
            data.setDataType(spec.getDataType());
            data.setGenres(spec.getGenres() == null ? null : new HashSet<>(spec.getGenres()));
        }
        DoubanMovie.DoubanMovieFaves faves = movie.getFaves();
        if (faves != null) {
            data.setFavesRemark(faves.getRemark());
            data.setFavesCreateTime(faves.getCreateTime());
            data.setFavesScore(faves.getScore());
            data.setFavesStatus(faves.getStatus());
        }
        return data;
    }
}

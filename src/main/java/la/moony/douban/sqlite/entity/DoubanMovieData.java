package la.moony.douban.sqlite.entity;

import java.time.Instant;
import java.util.Set;
import lombok.Data;

@Data
public class DoubanMovieData {

    private String id;
    private Instant creationTimestamp;

    private String name;
    private String poster;
    private String link;
    private String doubanId;
    private String score;
    private String year;
    private String type;
    private String pubdate;
    private String cardSubtitle;
    private String dataType;
    private Set<String> genres;

    private String favesRemark;
    private Instant favesCreateTime;
    private String favesScore;
    private String favesStatus;
}

package la.moony.douban;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DoubanRequest {
    private String userId;
    private Integer count;
    private Integer start;
    private DoubanType type;
    private DoubanStatus status;

    public enum DoubanType{
        movie,music,book,game,drama;
    }
    public enum DoubanStatus{
        mark,doing,done;
    }

}

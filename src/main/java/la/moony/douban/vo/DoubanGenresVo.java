package la.moony.douban.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DoubanGenresVo {
    String name;

    Integer doubanCount;
}

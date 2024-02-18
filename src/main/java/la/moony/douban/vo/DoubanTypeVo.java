package la.moony.douban.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DoubanTypeVo {

    String name;

    String key;

    Integer doubanCount;
}

package la.moony.douban.sqlite;

import java.util.List;

public record SqlitePage<T>(List<T> items, boolean hasNext) {
}

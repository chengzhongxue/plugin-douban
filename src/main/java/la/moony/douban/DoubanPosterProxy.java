package la.moony.douban;

import la.moony.douban.sqlite.entity.DoubanMovieData;
import org.apache.commons.lang3.StringUtils;

/**
 * 豆瓣海报反代：读出时替换域名，写回时恢复库内原始地址。
 */
public final class DoubanPosterProxy {

    private static final String DOUBAN_IMG_HOST_PATTERN = "https://img\\d+.doubanio.com";

    private DoubanPosterProxy() {
    }

    public static void applyProxy(DoubanMovieData data, SettingConfig.BaseConfig baseConfig) {
        if (data == null || data.getPoster() == null || baseConfig == null) {
            return;
        }
        if (!"halo".equals(data.getDataType()) && Boolean.TRUE.equals(baseConfig.getIsProxy())
            && StringUtils.isNotEmpty(baseConfig.getProxyHost())) {
            data.setPoster(data.getPoster()
                .replaceAll(DOUBAN_IMG_HOST_PATTERN, baseConfig.getProxyHost()));
        }
    }

    /**
     * 编辑保存时，若封面仍是反代后的地址，则写回数据库中的原始海报 URL，避免把反代域名入库。
     */
    public static void preserveOriginalPosterIfProxied(DoubanMovieData body,
        DoubanMovieData existing, SettingConfig.BaseConfig baseConfig) {
        if (body == null || existing == null || baseConfig == null
            || body.getPoster() == null || existing.getPoster() == null) {
            return;
        }
        if ("halo".equals(existing.getDataType())
            || !Boolean.TRUE.equals(baseConfig.getIsProxy())
            || StringUtils.isEmpty(baseConfig.getProxyHost())) {
            return;
        }
        String proxyHost = baseConfig.getProxyHost();
        String proxiedExisting = existing.getPoster()
            .replaceAll(DOUBAN_IMG_HOST_PATTERN, proxyHost);
        if (body.getPoster().equals(proxiedExisting) || body.getPoster().startsWith(proxyHost)) {
            body.setPoster(existing.getPoster());
        }
    }
}

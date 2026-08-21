package la.moony.douban.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import la.moony.douban.DoubanMovieQuery;
import la.moony.douban.DoubanPosterProxy;
import la.moony.douban.SettingConfig;
import la.moony.douban.service.DoubanService;
import la.moony.douban.sqlite.SqliteDoubanMovieStore;
import la.moony.douban.sqlite.SqlitePage;
import la.moony.douban.sqlite.entity.DoubanMovieData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.extension.ListResult;

@Component
public class DoubanServiceImpl implements DoubanService {

    private final Logger log = LoggerFactory.getLogger(DoubanServiceImpl.class);
    private static final String DB_API_LIST_URL1 =
        "https://fatesinger.com/dbapi/user/%s/interests?count=%s&start=%s&type=%s&status=%s";
    private static final String DB_API_DETAIL_URL =
        "https://fatesinger.com/dbapi/{type}/{id}?ck=xgtY&for_mobile=1";
    private static final String TMDB_API_URL =
        "https://hk.fatesinger.com/api/{type}/{tmdbId}?api_key={apiKey}&language=zh-CN";

    private final SqliteDoubanMovieStore movieStore;
    private final SettingConfig settingConfig;

    public DoubanServiceImpl(SqliteDoubanMovieStore movieStore, SettingConfig settingConfig) {
        this.movieStore = movieStore;
        this.settingConfig = settingConfig;
    }

    @Override
    public void synchronizationDouban() {
        settingConfig.getBaseConfig().flatMap(baseConfig -> {
            if (StringUtils.isNotEmpty(baseConfig.getDoubanId())) {
                addDouban(baseConfig.getDoubanId());
            }
            return Mono.empty();
        }).subscribe();
    }

    public void addDouban(String doubanId) {
        String[] types = {"movie", "music", "book", "game", "drama"};
        String[] status = {"done", "doing", "mark"};
        log.info("豆瓣开始抓取数据");
        for (String type : types) {
            for (String s : status) {
                AtomicBoolean condition = new AtomicBoolean(true);
                AtomicInteger i = new AtomicInteger(0);
                while (condition.get()) {
                    String baseUrl =
                        String.format(DB_API_LIST_URL1, doubanId, 49, 49 * i.get(), type, s);
                    ArrayNode arrayNode = listDouban(baseUrl);
                    if (arrayNode.isEmpty()) {
                        condition.set(false);
                    } else {
                        for (JsonNode node : arrayNode) {
                            JsonNode subject = node.get("subject");
                            String name = subject.get("title").asText();
                            String poster = subject.get("pic").get("large").asText();
                            String id = subject.get("id").asText();
                            String doubanScore = subject.get("rating").get("value").asText();
                            String link = subject.get("url").asText();
                            String year = "";
                            if (subject.get("year") != null) {
                                year = subject.get("year").asText();
                            }
                            String pubdate = "";
                            if (subject.get("pubdate").isArray() && subject.get("pubdate").size() > 0) {
                                pubdate = subject.get("pubdate").get(0).asText("");
                            }
                            String cardSubtitle = subject.get("card_subtitle").asText();
                            Set<String> genres = new HashSet<>();
                            if (subject.get("genres") != null) {
                                subject.get("genres").forEach(genre -> genres.add(genre.asText()));
                            }
                            String createTime = node.get("create_time").asText();
                            SimpleDateFormat dateFormat =
                                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            Date date;
                            try {
                                date = dateFormat.parse(createTime);
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            String remark = node.get("comment").asText();
                            String score;
                            if (!node.get("rating").isEmpty()) {
                                score = node.get("rating").get("value").asText();
                            } else {
                                score = "";
                            }
                            String status1 = node.get("status").asText();
                            var existing = movieStore.findByTypeAndDoubanId(type, id, null);
                            if (existing.isPresent()) {
                                DoubanMovieData doubanMovie = existing.get();
                                if (StringUtils.isNotEmpty(doubanMovie.getFavesStatus())
                                    && doubanMovie.getFavesStatus().equals(status1)) {
                                    condition.set(false);
                                    continue;
                                }
                                doubanMovie.setFavesCreateTime(date.toInstant());
                                doubanMovie.setFavesRemark(remark);
                                doubanMovie.setFavesScore(score);
                                doubanMovie.setFavesStatus(status1);
                                movieStore.update(doubanMovie);
                            } else {
                                DoubanMovieData doubanMovie = new DoubanMovieData();
                                doubanMovie.setId(UUID.randomUUID().toString());
                                doubanMovie.setCreationTimestamp(Instant.now());
                                doubanMovie.setName(name);
                                doubanMovie.setPoster(poster);
                                doubanMovie.setDoubanId(id);
                                doubanMovie.setScore(doubanScore);
                                doubanMovie.setLink(link);
                                doubanMovie.setYear(year);
                                doubanMovie.setType(type);
                                doubanMovie.setPubdate(pubdate);
                                doubanMovie.setCardSubtitle(cardSubtitle);
                                doubanMovie.setGenres(genres);
                                doubanMovie.setDataType("db");
                                doubanMovie.setFavesCreateTime(date.toInstant());
                                doubanMovie.setFavesRemark(remark);
                                doubanMovie.setFavesScore(score);
                                doubanMovie.setFavesStatus(status1);
                                movieStore.create(doubanMovie);
                            }
                        }
                        i.set(i.get() + 1);
                    }
                }
            }
        }
        log.info("豆瓣结束抓取数据");
    }

    @Override
    public Mono<DoubanMovieData> getDoubanDetail(String url) {
        Map<String, Object> matcher = matcher(url);
        String type = (String) matcher.get("type");
        String id = (String) matcher.get("id");
        int index = (int) matcher.get("index");
        if (StringUtils.isNotEmpty(type) && StringUtils.isNotEmpty(id)) {
            return switch (index) {
                case 1 -> embedHandlerDoubanlist(type, id);
                case 2 -> embedHandlerDoubanablum(type, id);
                case 3 -> embedHandlerDoubandrama(type, id);
                case 4 -> embedHandlerTheMovieDb(type, id);
                default -> Mono.just(new DoubanMovieData());
            };
        }
        return Mono.just(new DoubanMovieData());
    }

    @Override
    public Flux<String> listAllGenres(String type) {
        return Mono.fromCallable(() -> movieStore.listDistinctGenres(type))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<ListResult<DoubanMovieData>> listDoubanMovie(DoubanMovieQuery query) {
        return settingConfig.getBaseConfig()
            .flatMap(baseConfig -> Mono.fromCallable(() -> {
                    SqliteDoubanMovieStore.MovieQuery movieQuery = query.toMovieQuery();
                    SqlitePage<DoubanMovieData> slice = movieStore.listPage(
                        movieQuery, query.getPage(), query.getSize(), query.toSort());
                    long total = movieStore.count(movieQuery);
                    List<DoubanMovieData> items = slice.items().stream()
                        .map(data -> {
                            DoubanPosterProxy.applyProxy(data, baseConfig);
                            return data;
                        })
                        .toList();
                    return new ListResult<>(query.getPage(), query.getSize(), total, items);
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<DoubanMovieData> embedHandlerDoubanlist(String type, String id) {
        if (StringUtils.contains("movie,book,music", type)) {
            return doubanDetail(type, id);
        }
        return Mono.just(new DoubanMovieData());
    }

    public Mono<DoubanMovieData> embedHandlerDoubanablum(String type, String id) {
        if (StringUtils.contains("game", type)) {
            return doubanDetail(type, id);
        }
        return Mono.just(new DoubanMovieData());
    }

    public Mono<DoubanMovieData> embedHandlerDoubandrama(String type, String id) {
        if (StringUtils.contains("drama", type)) {
            return doubanDetail(type, id);
        }
        return Mono.just(new DoubanMovieData());
    }

    public Mono<DoubanMovieData> embedHandlerTheMovieDb(String type, String id) {
        if (StringUtils.contains("tv,movie", type)) {
            return settingConfig.getBaseConfig().flatMap(baseConfig -> {
                if (StringUtils.isNotEmpty(baseConfig.getApiKey())) {
                    return tmdbDetail(type, id, baseConfig.getApiKey());
                }
                return Mono.just(new DoubanMovieData());
            });
        }
        return Mono.just(new DoubanMovieData());
    }

    public Mono<DoubanMovieData> tmdbDetail(String type, String id, String apiKey) {
        return Mono.fromCallable(() -> movieStore.findByTypeAndDoubanId(type, id, "tmdb"))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(optional -> {
                if (optional.isPresent()) {
                    return Mono.just(optional.get());
                }
                return tmdbDetailRequest(type, id, apiKey).flatMap(jsonNode -> {
                    String name = jsonNode.get("title") != null
                        ? jsonNode.get("title").asText()
                        : jsonNode.get("name").asText();
                    String poster =
                        "https://image.tmdb.org/t/p/original" + jsonNode.get("poster_path").asText();
                    String tmdbId = jsonNode.get("id").asText();
                    String doubanScore = jsonNode.get("vote_average").asText();
                    String link = jsonNode.get("homepage").asText();
                    String year = "";
                    String pubdate = jsonNode.get("release_date") != null
                        ? jsonNode.get("release_date").asText()
                        : jsonNode.get("first_air_date").asText();
                    String cardSubtitle = jsonNode.get("overview").asText();
                    Set<String> genres = new HashSet<>();
                    if (jsonNode.get("genres") != null) {
                        jsonNode.get("genres")
                            .forEach(genre -> genres.add(genre.get("name").asText()));
                    }
                    DoubanMovieData data = new DoubanMovieData();
                    data.setId(UUID.randomUUID().toString());
                    data.setCreationTimestamp(Instant.now());
                    data.setName(name);
                    data.setPoster(poster);
                    data.setDoubanId(tmdbId);
                    data.setScore(doubanScore);
                    data.setLink(link);
                    data.setYear(year);
                    data.setType(type);
                    data.setPubdate(pubdate);
                    data.setCardSubtitle(cardSubtitle);
                    data.setGenres(genres);
                    data.setDataType("tmdb");
                    data.setFavesCreateTime(Instant.now());
                    return Mono.fromCallable(() -> movieStore.create(data))
                        .subscribeOn(Schedulers.boundedElastic());
                }).onErrorResume(WebClientResponseException.NotFound.class, error -> {
                    log.error("Resource not found: {}", error.getMessage());
                    return Mono.just(new DoubanMovieData());
                });
            });
    }

    public Mono<DoubanMovieData> doubanDetail(String type, String id) {
        return settingConfig.getBaseConfig().flatMap(base ->
            Mono.fromCallable(() -> movieStore.findByTypeAndDoubanId(type, id, null))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> {
                    if (optional.isPresent()) {
                        DoubanMovieData data = optional.get();
                        DoubanPosterProxy.applyProxy(data, base);
                        return Mono.just(data);
                    }
                    return doubanDetailRequest(type, id).flatMap(jsonNode -> {
                        String name = jsonNode.get("title").asText();
                        String poster = jsonNode.get("pic").get("large").asText();
                        String doubanId = jsonNode.get("id").asText();
                        String doubanScore = jsonNode.get("rating").get("value").asText();
                        String link = jsonNode.get("url").asText();
                        String year = "";
                        if (jsonNode.get("year") != null) {
                            year = jsonNode.get("year").asText();
                        }
                        String pubdate = "";
                        if (jsonNode.get("pubdate") != null
                            && jsonNode.get("pubdate").isArray()
                            && jsonNode.get("pubdate").size() > 0) {
                            pubdate = jsonNode.get("pubdate").get(0).asText("");
                        }
                        String cardSubtitle = jsonNode.get("card_subtitle").asText();
                        Set<String> genres = new HashSet<>();
                        if (jsonNode.get("genres") != null) {
                            jsonNode.get("genres").forEach(genre -> genres.add(genre.asText()));
                        }
                        DoubanMovieData data = new DoubanMovieData();
                        data.setId(UUID.randomUUID().toString());
                        data.setCreationTimestamp(Instant.now());
                        data.setName(name);
                        data.setPoster(poster);
                        data.setDoubanId(doubanId);
                        data.setScore(doubanScore);
                        data.setLink(link);
                        data.setYear(year);
                        data.setType(type);
                        data.setPubdate(pubdate);
                        data.setCardSubtitle(cardSubtitle);
                        data.setGenres(genres);
                        data.setDataType("db");
                        data.setFavesCreateTime(Instant.now());
                        return Mono.fromCallable(() -> movieStore.create(data))
                            .subscribeOn(Schedulers.boundedElastic())
                            .map(created -> {
                                DoubanPosterProxy.applyProxy(created, base);
                                return created;
                            });
                    }).onErrorResume(WebClientResponseException.NotFound.class, error -> {
                        log.error("Resource not found: {}", error.getMessage());
                        return Mono.just(new DoubanMovieData());
                    });
                }));
    }

    public Map<String, Object> matcher(String url) {
        Map<String, Object> map = new HashMap<>();
        String[] patterns = {
            "https?://(\\w+)\\.douban\\.com/subject/(\\d+)",
            "https?://www\\.douban\\.com/(\\w+)/(\\d+)",
            "https?://www\\.douban\\.com/location/(\\w+)/(\\d+)",
            "https?://www\\.themoviedb\\.org/(\\w+)/(\\d+)"
        };
        int index = 0;
        for (String regex : patterns) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(url);
            String type = null;
            String id = null;
            index = index + 1;
            if (matcher.find()) {
                type = matcher.group(1);
                id = matcher.group(2);
                map.put("type", type);
                map.put("id", id);
                map.put("index", index);
                return map;
            } else {
                log.info("No match found {}", url);
            }
            map.put("type", type);
            map.put("id", id);
            map.put("index", index);
        }
        return map;
    }

    public ArrayNode listDouban(String url) {
        String result;
        ArrayNode jsonNodes = new ArrayNode(null);
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in =
                new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            result = response.toString();

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(result);
            if (jsonNode.isObject()) {
                ObjectNode objectNode = (ObjectNode) jsonNode;
                jsonNodes = objectNode.withArray("/interests");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonNodes;
    }

    public Mono<JsonNode> doubanDetailRequest(String type, String id) {
        return WebClient.create().get()
            .uri(DB_API_DETAIL_URL, type, id)
            .retrieve()
            .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> tmdbDetailRequest(String type, String tmdbId, String apiKey) {
        return WebClient.create().get()
            .uri(TMDB_API_URL, type, tmdbId, apiKey)
            .retrieve()
            .bodyToMono(JsonNode.class);
    }
}

package la.moony.douban.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Nonnull;
import la.moony.douban.DoubanMovieQuery;
import la.moony.douban.DoubanRequest;
import la.moony.douban.extension.DoubanMovie;
import la.moony.douban.service.DoubanService;
import la.moony.douban.vo.DoubanMovieVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.plugin.ReactiveSettingFetcher;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.equal;

@Component
public class DoubanServiceImpl  implements DoubanService {

    private final Logger log = LoggerFactory.getLogger(DoubanServiceImpl.class);

    private final ExtensionClient client;
    private final ReactiveExtensionClient reactiveClient;

    private final WebClient webClient = WebClient.builder().build();
    private static final String DB_API_LIST_URL = "https://fatesinger.com/dbapi/user/{userid}/interests?count={count}&start={start}&type={type}&status={status}";

    private static final String DB_API_DETAIL_URL = "https://fatesinger.com/dbapi/{type}/{id}?ck=xgtY&for_mobile=1";
    private static final String TMDB_API_URL = "https://hk.fatesinger.com/api/{type}/{tmdbId}?api_key={apiKey}&language=zh-CN";

    private final ReactiveSettingFetcher settingFetcher;

    public DoubanServiceImpl(ExtensionClient client, ReactiveExtensionClient reactiveClient,
        ReactiveSettingFetcher settingFetcher) {
        this.client = client;
        this.reactiveClient = reactiveClient;
        this.settingFetcher = settingFetcher;
    }



    @Override
    public void synchronizationDouban() {
        String DOUBAN_ID = getDoubanId();
        if (StringUtils.isNotEmpty(DOUBAN_ID)){
            addDouban(DOUBAN_ID);
        }

    }

    public void addDouban(String DOUBAN_ID){
        String[] types = {"movie","music","book", "game", "drama"};
        String[] status = {"done","doing","mark"};
        log.info("豆瓣开始抓取数据");
        for (String type : types) {
            for (String s : status) {
                boolean confition = true;
                int i =0;
                while (confition) {
                    ArrayNode arrayNode = listDouban(DoubanRequest.builder()
                        .userId(DOUBAN_ID)
                        .count(49)
                        .start(49 * i)
                        .type(DoubanRequest.DoubanType.valueOf(type))
                        .status(DoubanRequest.DoubanStatus.valueOf(s))
                        .build()).block();
                    if (arrayNode.isEmpty()){
                        confition = false;
                    }else {
                        for(JsonNode node : arrayNode){
                            JsonNode subject = node.get("subject");
                            String name = subject.get("title").asText();
                            String poster = subject.get("pic").get("large").asText();
                            String id = subject.get("id").asText();
                            String doubanScore = subject.get("rating").get("value").asText();
                            String link = subject.get("url").asText();
                            String year = "";
                            if (subject.get("year")!=null){
                                year =subject.get("year").asText();
                            }
                            String type1 = type;
                            String pubdate = "";
                            if (subject.get("pubdate").isArray() && subject.get("pubdate").size()>0){
                                pubdate = subject.get("pubdate").get(0).asText("");
                            }
                            String cardSubtitle = subject.get("card_subtitle").asText();
                            Set<String> genres = new HashSet();
                            if (subject.get("genres")!=null){
                                subject.get("genres").forEach(genre -> {
                                    genres.add(genre.asText());
                                });
                            }
                            String createTime = node.get("create_time").asText();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 定义日期格式
                            Date date = null;
                            try {
                                date = dateFormat.parse(createTime); // 将字符串转换为Date类型
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            String remark = node.get("comment").asText();
                            String score = "";
                            if (!node.get("rating").isEmpty()){
                                score = node.get("rating").get("value").asText();
                            }
                            String status1 = node.get("status").asText();
                            var listOptions = new ListOptions();
                            var query = and(equal("spec.type", type1),equal("spec.id", id));
                            listOptions.setFieldSelector(FieldSelector.of(query));
                            List<DoubanMovie> doubanMovies = client.listAll(DoubanMovie.class, listOptions, null);
                            if (doubanMovies.size()==0){
                                DoubanMovie doubanMovie = new DoubanMovie();
                                doubanMovie.setMetadata(new Metadata());
                                doubanMovie.getMetadata().setGenerateName("douban-movie-");
                                doubanMovie.setSpec(new DoubanMovie.DoubanMovieSpec());
                                doubanMovie.getSpec().setName(name);
                                doubanMovie.getSpec().setPoster(poster);
                                doubanMovie.getSpec().setId(id);
                                doubanMovie.getSpec().setScore(doubanScore);
                                doubanMovie.getSpec().setLink(link);
                                doubanMovie.getSpec().setYear(year);
                                doubanMovie.getSpec().setType(type1);
                                doubanMovie.getSpec().setPubdate(pubdate);
                                doubanMovie.getSpec().setCardSubtitle(cardSubtitle);
                                doubanMovie.getSpec().setGenres(genres);
                                doubanMovie.getSpec().setDataType("db");
                                doubanMovie.setFaves(new DoubanMovie.DoubanMovieFaves());
                                doubanMovie.getFaves().setCreateTime(date.toInstant());
                                doubanMovie.getFaves().setRemark(remark);
                                doubanMovie.getFaves().setScore(score);
                                doubanMovie.getFaves().setStatus(status1);
                                client.create(doubanMovie);
                            }else {
                                DoubanMovie doubanMovie = doubanMovies.get(0);
                                if (doubanMovie.getFaves().getStatus().equals(status1)){
                                    confition = false;
                                }else {
                                    doubanMovie.getFaves().setCreateTime(date.toInstant());
                                    doubanMovie.getFaves().setRemark(remark);
                                    doubanMovie.getFaves().setScore(score);
                                    doubanMovie.getFaves().setStatus(status1);
                                    client.update(doubanMovie);
                                }
                            }
                        }
                        i++;
                    }
                }
            }
        }
        log.info("豆瓣结束抓取数据");
    }


    @Override
    public Mono<DoubanMovieVo> getDoubanDetail(String url) {
        DoubanMovie doubanMovie = new DoubanMovie();
        Map<String, Object> matcher = matcher(url);
        String type = (String) matcher.get("type");
        String id =  (String) matcher.get("id");
        int index =  (int) matcher.get("index");
        if (StringUtils.isNotEmpty(type) && StringUtils.isNotEmpty(id)){
            switch (index){
                case 1:
                    return embedHandlerDoubanlist(type,id);
                case 2:
                    return embedHandlerDoubanablum(type,id);
                case 3:
                    return embedHandlerDoubandrama(type,id);
                case 4:
                    return embedHandlerTheMovieDb(type,id);
            }
            return getDoubanMovieVo(doubanMovie);
        }else {
            return getDoubanMovieVo(doubanMovie);
        }

    }

    @Override
    public Flux<String> listAllGenres() {
        return reactiveClient.listAll(DoubanMovie.class, new ListOptions(),
                Sort.by("metadata.name").descending())
            .flatMapIterable(doubanMovie -> {
                var genres = doubanMovie.getSpec().getGenres();
                return Objects.requireNonNullElseGet(genres, List::of);
            })
            .distinct();
    }

    @Override
    public Mono<ListResult<DoubanMovie>> listDoubanMovie(DoubanMovieQuery query) {
        return reactiveClient.listBy(DoubanMovie.class, query.toListOptions(), query.toPageRequest())
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .collectList()
                .map(list -> new ListResult<>(listResult.getPage(), listResult.getSize(),
                    listResult.getTotal(), list)
                )
            );
    }

    public Mono<DoubanMovieVo> embedHandlerDoubanlist(String type,String id){

        if (StringUtils.contains("movie,book,music", type)){
           return doubanDetail(type,id);
        }
        return getDoubanMovieVo(new DoubanMovie());
    }

    public Mono<DoubanMovieVo> embedHandlerDoubanablum(String type,String id){
        if (StringUtils.contains("game", type)){
            return doubanDetail(type,id);
        }
        return getDoubanMovieVo(new DoubanMovie());
    }

    public Mono<DoubanMovieVo> embedHandlerDoubandrama(String type,String id){
        if (StringUtils.contains("drama", type)){
            return doubanDetail(type,id);
        }
        return getDoubanMovieVo(new DoubanMovie());
    }

    public Mono<DoubanMovieVo> embedHandlerTheMovieDb(String type,String id){
        if (StringUtils.contains("tv,movie", type)){
            return getApiKey().flatMap(apiKey->{
                if (StringUtils.isNotEmpty(apiKey) ){
                    return tmdbDetail(type,id,apiKey);
                }
                return getDoubanMovieVo(new DoubanMovie());
            });
        }
        return getDoubanMovieVo(new DoubanMovie());
    }

    public Mono<DoubanMovieVo> tmdbDetail(String type,String id,String apiKey){
        var listOptions = new ListOptions();
        var query = and(equal("spec.type", type),equal("spec.id", id),equal("spec.dataType", "tmdb"));
        listOptions.setFieldSelector(FieldSelector.of(query));
        Flux<DoubanMovie> list = reactiveClient.listAll(DoubanMovie.class, listOptions, null);
        Mono<Boolean> booleanMono = list.hasElements();
        return booleanMono.flatMap(hasValue ->{
            if (hasValue){
                return (Mono<DoubanMovieVo>) list.next().flatMap(doubanMovie -> {
                    return getDoubanMovieVo(doubanMovie);
                });
            }else {
                DoubanMovie doubanMovieDetail = new DoubanMovie();
                return tmdbDetailRequest(type, id,apiKey).flatMap(jsonNode->{
                    String name = jsonNode.get("title")!=null ? jsonNode.get("title").asText() : jsonNode.get("name").asText();
                    String poster = "https://image.tmdb.org/t/p/original"+jsonNode.get("poster_path").asText();
                    String tmdbId = jsonNode.get("id").asText();
                    String doubanScore = jsonNode.get("vote_average").asText();
                    String link = jsonNode.get("homepage").asText();
                    String year = "";
                    String pubdate = jsonNode.get("release_date")!=null ? jsonNode.get("release_date").asText() : jsonNode.get("first_air_date").asText();
                    String cardSubtitle = jsonNode.get("overview").asText();
                    Set<String> genres = new HashSet();
                    if (jsonNode.get("genres")!=null){
                        jsonNode.get("genres").forEach(genre -> {
                            genres.add(genre.get("name").asText());
                        });
                    }
                    doubanMovieDetail.setMetadata(new Metadata());
                    doubanMovieDetail.getMetadata().setGenerateName("douban-movie-");
                    doubanMovieDetail.setSpec(new DoubanMovie.DoubanMovieSpec());
                    doubanMovieDetail.getSpec().setName(name);
                    doubanMovieDetail.getSpec().setPoster(poster);
                    doubanMovieDetail.getSpec().setId(tmdbId);
                    doubanMovieDetail.getSpec().setScore(doubanScore);
                    doubanMovieDetail.getSpec().setLink(link);
                    doubanMovieDetail.getSpec().setYear(year);
                    doubanMovieDetail.getSpec().setType(type);
                    doubanMovieDetail.getSpec().setPubdate(pubdate);
                    doubanMovieDetail.getSpec().setCardSubtitle(cardSubtitle);
                    doubanMovieDetail.getSpec().setGenres(genres);
                    doubanMovieDetail.getSpec().setDataType("tmdb");
                    doubanMovieDetail.setFaves(new DoubanMovie.DoubanMovieFaves());
                    doubanMovieDetail.getFaves().setCreateTime(Instant.now());
                    doubanMovieDetail.getFaves().setRemark(null);
                    doubanMovieDetail.getFaves().setScore(null);
                    doubanMovieDetail.getFaves().setStatus(null);
                    reactiveClient.create(doubanMovieDetail).subscribe();
                    return getDoubanMovieVo(doubanMovieDetail);
                }).onErrorResume(WebClientResponseException.NotFound.class, error -> {
                    log.error("Resource not found: ",error.getMessage());
                    return getDoubanMovieVo(doubanMovieDetail);
                });
            }
        });
    }

    public Mono<DoubanMovieVo> doubanDetail(String type,String id){
        var listOptions = new ListOptions();
        var query = and(equal("spec.type", type),equal("spec.id", id));
        listOptions.setFieldSelector(FieldSelector.of(query));
        Flux<DoubanMovie> list = reactiveClient.listAll(DoubanMovie.class, listOptions, null);
        Mono<Boolean> booleanMono = list.hasElements();
       return booleanMono.flatMap(hasValue ->{
            if (hasValue){
                return (Mono<DoubanMovieVo>) list.next().flatMap(doubanMovie -> {
                    return getDoubanMovieVo(doubanMovie);
                });
            }else {
               DoubanMovie doubanMovieDetail = new DoubanMovie();
              return doubanDetailRequest(type, id).flatMap(jsonNode->{
                   String name = jsonNode.get("title").asText();
                   String poster = jsonNode.get("pic").get("large").asText();
                   String doubanId = jsonNode.get("id").asText();
                   String doubanScore = jsonNode.get("rating").get("value").asText();
                   String link = jsonNode.get("url").asText();
                   String year = "";
                   if (jsonNode.get("year")!=null){
                       year =jsonNode.get("year").asText();
                   }
                   String pubdate = "";
                   if (jsonNode.get("pubdate")!=null){
                       if (jsonNode.get("pubdate").isArray() && jsonNode.get("pubdate").size()>0){
                           pubdate = jsonNode.get("pubdate").get(0).asText("");
                       }
                   }
                   String cardSubtitle = jsonNode.get("card_subtitle").asText();
                   Set<String> genres = new HashSet();
                   if (jsonNode.get("genres")!=null){
                       jsonNode.get("genres").forEach(genre -> {
                           genres.add(genre.asText());
                       });
                   }
                   doubanMovieDetail.setMetadata(new Metadata());
                   doubanMovieDetail.getMetadata().setGenerateName("douban-movie-");
                   doubanMovieDetail.setSpec(new DoubanMovie.DoubanMovieSpec());
                   doubanMovieDetail.getSpec().setName(name);
                   doubanMovieDetail.getSpec().setPoster(poster);
                   doubanMovieDetail.getSpec().setId(doubanId);
                   doubanMovieDetail.getSpec().setScore(doubanScore);
                   doubanMovieDetail.getSpec().setLink(link);
                   doubanMovieDetail.getSpec().setYear(year);
                   doubanMovieDetail.getSpec().setType(type);
                   doubanMovieDetail.getSpec().setPubdate(pubdate);
                   doubanMovieDetail.getSpec().setCardSubtitle(cardSubtitle);
                   doubanMovieDetail.getSpec().setGenres(genres);
                   doubanMovieDetail.getSpec().setDataType("db");
                   doubanMovieDetail.setFaves(new DoubanMovie.DoubanMovieFaves());
                   doubanMovieDetail.getFaves().setCreateTime(Instant.now());
                   doubanMovieDetail.getFaves().setRemark(null);
                   doubanMovieDetail.getFaves().setScore(null);
                   doubanMovieDetail.getFaves().setStatus(null);
                   reactiveClient.create(doubanMovieDetail).subscribe();
                   return getDoubanMovieVo(doubanMovieDetail);
              }).onErrorResume(WebClientResponseException.NotFound.class, error -> {
                  log.error("Resource not found: ",error.getMessage());
                  return getDoubanMovieVo(doubanMovieDetail);
              });
            }
        });
    }

    public Map<String,Object> matcher(String url){
        Map<String,Object> map = new HashMap<>();
        String[] patterns = {
            "https?://(\\w+)\\.douban\\.com/subject/(\\d+)",
            "https?://www\\.douban\\.com/(\\w+)/(\\d+)",
            "https?://www\\.douban\\.com/location/(\\w+)/(\\d+)",
            "https?://www\\.themoviedb\\.org/(\\w+)/(\\d+)"
        };
        String input = url;
        int index = 0;
        for (String regex : patterns) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(input);
            String type = null;
            String id = null;
            index = index+1;
            if (matcher.find()) {
                type = matcher.group(1);
                id = matcher.group(2);
                map.put("type",type);
                map.put("id",id);
                map.put("index",index);
                return map;
            }else {
                log.info("No match found {}",url);
            }
            map.put("type",type);
            map.put("id",id);
            map.put("index",index);
        }
        return map;
    }

    String getDoubanId() {
        return this.settingFetcher.get("base")
            .map(setting -> setting.get("doubanId").asText()).block();
    }

    Mono<String> getApiKey() {
        return this.settingFetcher.get("base")
            .map(setting -> setting.get("apiKey").asText());
    }

    private Mono<DoubanMovieVo> getDoubanMovieVo(@Nonnull DoubanMovie doubanMovie) {
        DoubanMovieVo doubanMovieVo = DoubanMovieVo.from(doubanMovie);
        return Mono.just(doubanMovieVo);
    }

    @Override
    public Mono<ArrayNode> listDouban(DoubanRequest request) {
        return webClient.get()
            .uri(DB_API_LIST_URL, request.getUserId(),request.getCount(),request.getStart(),request.getType().name(),request.getStatus().name())
            .retrieve()
            .bodyToMono(ObjectNode.class)
            .map(item->{
                return item.withArray("/interests");
            });
    }

    public Mono<JsonNode> doubanDetailRequest(String type,String id) {
        return WebClient.create().get()
            .uri(DB_API_DETAIL_URL, type,id)
            .retrieve()
            .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> tmdbDetailRequest(String type,String tmdbId,String apiKey) {
        return WebClient.create().get()
            .uri(TMDB_API_URL, type,tmdbId,apiKey)
            .retrieve()
            .bodyToMono(JsonNode.class);
    }

}

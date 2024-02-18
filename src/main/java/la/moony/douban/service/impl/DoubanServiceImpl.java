package la.moony.douban.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import la.moony.douban.DoubanRequest;
import la.moony.douban.extension.DoubanMovie;
import la.moony.douban.service.DoubanService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.Metadata;
import run.halo.app.plugin.ReactiveSettingFetcher;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

@Component
public class DoubanServiceImpl  implements DoubanService {

    private final Logger log = LoggerFactory.getLogger(DoubanServiceImpl.class);

    private final ExtensionClient client;

    private final WebClient webClient = WebClient.builder().build();
    private static final String DB_API_URL = "https://fatesinger.com/dbapi/user/{userid}/interests?count={count}&start={start}&type={type}&status={status}";

    private static final String TMDB_API_URL = "https://hk.fatesinger.com/api/{type}/{tmdbId}?api_key={apiKey}&language=zh-CN";

    private final ReactiveSettingFetcher settingFetcher;

    public DoubanServiceImpl(ExtensionClient client, ReactiveSettingFetcher settingFetcher) {
        this.client = client;
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
                            Predicate<DoubanMovie> predicate = doubanMovie -> doubanMovie.getSpec().getType().equals(type1) && doubanMovie.getSpec().getId().equals(id);
                            List<DoubanMovie> doubanMovies = client.list(DoubanMovie.class, predicate, null);
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
    public Mono<ArrayNode> listDouban(DoubanRequest request) {
        return webClient.get()
            .uri(DB_API_URL, request.getUserId(),request.getCount(),request.getStart(),request.getType().name(),request.getStatus().name())
            .retrieve()
            .bodyToMono(ObjectNode.class)
            .map(item->{
                return item.withArray("/interests");
            });
    }

    String getDoubanId() {
        return this.settingFetcher.get("base")
            .map(setting -> setting.get("doubanId").asText()).block();
    }
}

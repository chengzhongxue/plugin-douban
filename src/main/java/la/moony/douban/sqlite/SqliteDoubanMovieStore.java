package la.moony.douban.sqlite;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import la.moony.douban.sqlite.entity.DoubanMovieData;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@RequiredArgsConstructor
public class SqliteDoubanMovieStore {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Set<String>> SET_TYPE = new TypeReference<>() {
    };

    private final DoubanSqliteDatabase database;

    public DoubanMovieData create(DoubanMovieData record) {
        if (StringUtils.isBlank(record.getId())) {
            record.setId(UUID.randomUUID().toString());
        }
        if (record.getCreationTimestamp() == null) {
            record.setCreationTimestamp(Instant.now());
        }
        return database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO douban_movies(
                  id, creation_timestamp, name, poster, link, douban_id, score, year,
                  type, pubdate, card_subtitle, data_type, genres, faves_remark, faves_create_time,
                  faves_score, faves_status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
                bindMovie(statement, record);
                statement.executeUpdate();
            }
            return record;
        });
    }

    public DoubanMovieData update(DoubanMovieData record) {
        if (StringUtils.isBlank(record.getId())) {
            throw new IllegalArgumentException("id is required");
        }
        return database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE douban_movies SET
                  creation_timestamp = ?, name = ?, poster = ?, link = ?,
                  douban_id = ?, score = ?, year = ?, type = ?, pubdate = ?, card_subtitle = ?,
                  data_type = ?, genres = ?, faves_remark = ?, faves_create_time = ?,
                  faves_score = ?, faves_status = ?
                WHERE id = ?
                """)) {
                int index = 1;
                statement.setString(index++, InstantUtils.format(record.getCreationTimestamp()));
                statement.setString(index++, record.getName());
                statement.setString(index++, record.getPoster());
                statement.setString(index++, record.getLink());
                statement.setString(index++, record.getDoubanId());
                statement.setString(index++, record.getScore());
                statement.setString(index++, record.getYear());
                statement.setString(index++, record.getType());
                statement.setString(index++, record.getPubdate());
                statement.setString(index++, record.getCardSubtitle());
                statement.setString(index++, record.getDataType());
                setJson(statement, index++, record.getGenres());
                statement.setString(index++, record.getFavesRemark());
                statement.setString(index++, InstantUtils.format(record.getFavesCreateTime()));
                statement.setString(index++, record.getFavesScore());
                statement.setString(index++, record.getFavesStatus());
                statement.setString(index, record.getId());
                if (statement.executeUpdate() == 0) {
                    throw new IllegalArgumentException("DoubanMovie not found: " + record.getId());
                }
            }
            return record;
        });
    }

    public Optional<DoubanMovieData> findById(String id) {
        return database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM douban_movies WHERE id = ?")) {
                statement.setString(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    public Optional<DoubanMovieData> findByTypeAndDoubanId(String type, String doubanId,
        String dataType) {
        return database.execute(connection -> {
            String sql = dataType == null
                ? "SELECT * FROM douban_movies WHERE type = ? AND douban_id = ? LIMIT 1"
                : "SELECT * FROM douban_movies WHERE type = ? AND douban_id = ? AND data_type = ? LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, type);
                statement.setString(2, doubanId);
                if (dataType != null) {
                    statement.setString(3, dataType);
                }
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    public boolean deleteById(String id) {
        return database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM douban_movies WHERE id = ?")) {
                statement.setString(1, id);
                return statement.executeUpdate() > 0;
            }
        });
    }

    public void deleteAll() {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM douban_movies")) {
                statement.executeUpdate();
            }
            return null;
        });
    }

    public SqlitePage<DoubanMovieData> listPage(MovieQuery query, int page, int size, Sort sort) {
        return database.execute(connection -> {
            int safePage = Math.max(page, 1);
            int safeSize = Math.max(size, 1);
            StringBuilder sql = new StringBuilder("SELECT * FROM douban_movies WHERE 1=1");
            List<Object> params = new ArrayList<>();
            appendFilters(sql, params, query);
            sql.append(' ').append(orderBy(sort));
            sql.append(" LIMIT ? OFFSET ?");
            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                int index = bindParams(statement, params, 1);
                statement.setInt(index++, safeSize + 1);
                statement.setLong(index, (long) (safePage - 1) * (long) safeSize);
                List<DoubanMovieData> records = new ArrayList<>();
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        records.add(mapRow(rs));
                    }
                }
                boolean hasNext = records.size() > safeSize;
                List<DoubanMovieData> items = hasNext
                    ? new ArrayList<>(records.subList(0, safeSize))
                    : records;
                return new SqlitePage<>(items, hasNext);
            }
        });
    }

    public long count(MovieQuery query) {
        return database.execute(connection -> {
            StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM douban_movies WHERE 1=1");
            List<Object> params = new ArrayList<>();
            appendFilters(sql, params, query);
            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                bindParams(statement, params, 1);
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
    }

    public List<DoubanMovieData> listAll(MovieQuery query, Sort sort) {
        return database.execute(connection -> {
            StringBuilder sql = new StringBuilder("SELECT * FROM douban_movies WHERE 1=1");
            List<Object> params = new ArrayList<>();
            appendFilters(sql, params, query);
            sql.append(' ').append(orderBy(sort));
            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                bindParams(statement, params, 1);
                List<DoubanMovieData> records = new ArrayList<>();
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        records.add(mapRow(rs));
                    }
                }
                return records;
            }
        });
    }

    public List<String> listDistinctGenres(String type) {
        return database.execute(connection -> {
            StringBuilder sql = new StringBuilder(
                "SELECT genres FROM douban_movies WHERE genres IS NOT NULL AND genres != ''");
            List<Object> params = new ArrayList<>();
            if (StringUtils.isNotBlank(type)) {
                sql.append(" AND type = ?");
                params.add(type);
            }
            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                bindParams(statement, params, 1);
                Set<String> genres = new LinkedHashSet<>();
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        Set<String> parsed = readJsonSet(rs.getString(1));
                        if (parsed != null) {
                            genres.addAll(parsed);
                        }
                    }
                }
                return new ArrayList<>(genres);
            }
        });
    }

    public List<GenreCount> listGenreCounts(String type, boolean requireStatus) {
        MovieQuery query = new MovieQuery();
        query.setType(type);
        query.setRequireStatus(requireStatus);
        List<DoubanMovieData> movies = listAll(query, Sort.by(Sort.Order.desc("faves_create_time")));
        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (DoubanMovieData movie : movies) {
            if (CollectionUtils.isEmpty(movie.getGenres())) {
                continue;
            }
            for (String genre : movie.getGenres()) {
                counts.merge(genre, 1, Integer::sum);
            }
        }
        List<GenreCount> result = new ArrayList<>();
        counts.forEach((name, count) -> result.add(new GenreCount(name, count)));
        return result;
    }

    public List<TypeCount> listTypeCounts(boolean requireStatus) {
        MovieQuery query = new MovieQuery();
        query.setRequireStatus(requireStatus);
        List<DoubanMovieData> movies = listAll(query, Sort.by(Sort.Order.desc("faves_create_time")));
        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (DoubanMovieData movie : movies) {
            if (StringUtils.isBlank(movie.getType())) {
                continue;
            }
            counts.merge(movie.getType(), 1, Integer::sum);
        }
        List<TypeCount> result = new ArrayList<>();
        counts.forEach((key, count) -> result.add(new TypeCount(key, count)));
        return result;
    }

    private static void appendFilters(StringBuilder sql, List<Object> params, MovieQuery query) {
        if (query == null) {
            return;
        }
        if (StringUtils.isNotBlank(query.getKeyword())) {
            sql.append(" AND name LIKE ?");
            params.add("%" + query.getKeyword() + "%");
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            sql.append(" AND faves_status = ?");
            params.add(query.getStatus());
        }
        if (query.isRequireStatus()) {
            sql.append(" AND faves_status IS NOT NULL AND faves_status != ''");
        }
        if (StringUtils.isNotBlank(query.getType())) {
            sql.append(" AND type = ?");
            params.add(query.getType());
        }
        if (StringUtils.isNotBlank(query.getDataType())) {
            sql.append(" AND data_type = ?");
            params.add(query.getDataType());
        }
        if (!CollectionUtils.isEmpty(query.getGenres())) {
            sql.append(" AND (");
            boolean first = true;
            for (String genre : query.getGenres()) {
                if (!first) {
                    sql.append(" OR ");
                }
                first = false;
                sql.append("genres LIKE ?");
                params.add("%\"" + genre + "\"%");
            }
            sql.append(')');
        }
        if (StringUtils.isNotBlank(query.getGenre())) {
            sql.append(" AND genres LIKE ?");
            params.add("%\"" + query.getGenre() + "\"%");
        }
    }

    private static String orderBy(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return "ORDER BY faves_create_time DESC, id DESC";
        }
        List<String> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            String column = mapSortProperty(order.getProperty());
            if (column == null) {
                continue;
            }
            orders.add(column + (order.isAscending() ? " ASC" : " DESC"));
        }
        if (orders.isEmpty()) {
            return "ORDER BY faves_create_time DESC, id DESC";
        }
        orders.add("id DESC");
        return "ORDER BY " + String.join(", ", orders);
    }

    private static String mapSortProperty(String property) {
        if (property == null) {
            return null;
        }
        return switch (property) {
            case "favesCreateTime", "faves_create_time" -> "faves_create_time";
            case "id" -> "id";
            case "name" -> "name";
            case "type" -> "type";
            default -> null;
        };
    }

    private static int bindParams(PreparedStatement statement, List<Object> params, int start)
        throws SQLException {
        int index = start;
        for (Object param : params) {
            statement.setObject(index++, param);
        }
        return index;
    }

    private static void bindMovie(PreparedStatement statement, DoubanMovieData record)
        throws SQLException {
        int index = 1;
        statement.setString(index++, record.getId());
        statement.setString(index++, InstantUtils.format(record.getCreationTimestamp()));
        statement.setString(index++, record.getName());
        statement.setString(index++, record.getPoster());
        statement.setString(index++, record.getLink());
        statement.setString(index++, record.getDoubanId());
        statement.setString(index++, record.getScore());
        statement.setString(index++, record.getYear());
        statement.setString(index++, record.getType());
        statement.setString(index++, record.getPubdate());
        statement.setString(index++, record.getCardSubtitle());
        statement.setString(index++, record.getDataType());
        setJson(statement, index++, record.getGenres());
        statement.setString(index++, record.getFavesRemark());
        statement.setString(index++, InstantUtils.format(record.getFavesCreateTime()));
        statement.setString(index++, record.getFavesScore());
        statement.setString(index, record.getFavesStatus());
    }

    private static DoubanMovieData mapRow(ResultSet rs) throws SQLException {
        DoubanMovieData data = new DoubanMovieData();
        data.setId(rs.getString("id"));
        data.setCreationTimestamp(InstantUtils.parse(rs.getString("creation_timestamp")));
        data.setName(rs.getString("name"));
        data.setPoster(rs.getString("poster"));
        data.setLink(rs.getString("link"));
        data.setDoubanId(rs.getString("douban_id"));
        data.setScore(rs.getString("score"));
        data.setYear(rs.getString("year"));
        data.setType(rs.getString("type"));
        data.setPubdate(rs.getString("pubdate"));
        data.setCardSubtitle(rs.getString("card_subtitle"));
        data.setDataType(rs.getString("data_type"));
        data.setGenres(readJsonSet(rs.getString("genres")));
        data.setFavesRemark(rs.getString("faves_remark"));
        data.setFavesCreateTime(InstantUtils.parse(rs.getString("faves_create_time")));
        data.setFavesScore(rs.getString("faves_score"));
        data.setFavesStatus(rs.getString("faves_status"));
        return data;
    }

    private static void setJson(PreparedStatement statement, int index, Object value)
        throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
            return;
        }
        try {
            statement.setString(index, OBJECT_MAPPER.writeValueAsString(value));
        } catch (Exception e) {
            throw new SQLException("Failed to serialize JSON", e);
        }
    }

    private static Set<String> readJsonSet(String value) throws SQLException {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            Set<String> set = OBJECT_MAPPER.readValue(value, SET_TYPE);
            return set == null ? null : new HashSet<>(set);
        } catch (Exception e) {
            throw new SQLException("Failed to parse genres JSON", e);
        }
    }

    @lombok.Data
    public static class MovieQuery {
        private String keyword;
        private String status;
        private String type;
        private String dataType;
        private String genre;
        private List<String> genres;
        private boolean requireStatus;
    }

    public record GenreCount(String name, int count) {
    }

    public record TypeCount(String key, int count) {
    }
}

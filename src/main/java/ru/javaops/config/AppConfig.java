package ru.javaops.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import ru.javaops.to.PayDetail;
import ru.javaops.util.JsonUtil;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static ru.javaops.service.CachedProjects.projectMap;

/**
 * GKislin
 */
@Configuration
public class AppConfig {

    @Profile("dev")
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2WebServer() throws SQLException {
        return Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082");
    }

    @Profile("prod")
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2Server() throws SQLException {
        return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092");
    }

    @Autowired
    public void configureJackson(ObjectMapper objectMapper) {
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        JsonUtil.setMapper(objectMapper);
    }

    public static volatile Properties sqlProps;
    public static volatile Properties infoSource;
    public static volatile Map<String, Map<String, PayDetail>> projectPayDetails;
    public static volatile Map<String, PayDetail> payDetails;

    @Scheduled(fixedRate = 10000)  // every 10 sec
    private void refreshSqlProps() {
        sqlProps = loadProps("./config/sql.properties");
        infoSource = loadProps("./config/infoSource.properties");
        payDetails = loadJson("./config/payDetails.json", new TypeReference<LinkedHashMap<String, PayDetail>>() {});
        projectPayDetails = payDetails.entrySet().stream().collect(
                Collectors.groupingBy(e -> projectMap.get(e.getKey().charAt(0)).getName(), LinkedHashMap::new,
                        Collector.of(LinkedHashMap::new, (map, e) -> {
                            PayDetail payDetail = e.getValue();
                            payDetail.setProject(projectMap.get(e.getKey().charAt(0)));
                            map.put(e.getKey(), payDetail);
                        }, (m1, m2) -> null /* called only for parallel*/)));
    }

    private static <T> Map<String, T> loadJson(String file, TypeReference<? extends Map<String, T>> typeReference) {
        Path path = Paths.get(file);
        try (Reader reader = Files.newBufferedReader(path)) {
            return JsonUtil.readValue(reader, typeReference);
        } catch (IOException e) {
            throw new IllegalStateException(path.toAbsolutePath().toString() + " load exception", e);
        }
    }

    private static Properties loadProps(String file) {
        Path path = Paths.get(file);
        try (Reader reader = Files.newBufferedReader(path)) {
            Properties props = new Properties();
            props.load(reader);
            return props;
        } catch (IOException e) {
            throw new IllegalStateException(path.toAbsolutePath().toString() + " load exception", e);
        }
    }
}

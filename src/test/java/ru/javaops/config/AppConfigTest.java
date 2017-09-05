package ru.javaops.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import ru.javaops.payment.ProjectPayDetail;
import ru.javaops.util.JsonUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * gkislin
 * 09.08.2017
 */
public class AppConfigTest {
    @Test
    public void refreshAppProps() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        JsonUtil.setMapper(objectMapper);

        Map<String, ProjectPayDetail> projectsPayDetailMap = AppConfig.loadJson("./config/payDetails.json", new TypeReference<LinkedHashMap<String, ProjectPayDetail>>() {
        });
        System.out.println(projectsPayDetailMap);
    }
}
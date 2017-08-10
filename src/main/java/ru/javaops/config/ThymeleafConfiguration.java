package ru.javaops.config;

import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

@Configuration
//http://www.thymeleaf.org/doc/articles/thymeleaf3migration.html
public class ThymeleafConfiguration implements SchedulingConfigurer {

    @Autowired
    private AppProperties properties;

    @Bean
    // Attention: with TemplateEngine clear cache doesn't work
    public SpringTemplateEngine thymeleafTemplateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolvers(ImmutableSet.of(
                new FileTemplateResolver() {{
                    setPrefix("./resources/view/");
                    setSuffix(".html");
                    setTemplateMode(TemplateMode.HTML);
                    setCharacterEncoding("UTF-8");
                    // https://github.com/spring-projects/spring-boot/issues/6500
                    setCheckExistence(true);
                    setOrder(1);
                }},
                new FileTemplateResolver() {{
                    setPrefix("./resources/mails/");
                    setSuffix(".html");
                    setTemplateMode(TemplateMode.HTML);
                    setCharacterEncoding("UTF-8");
                    setOrder(2);
                }}
        ));
        return engine;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedDelayTask(thymeleafTemplateEngine()::clearTemplateCache, properties.getCacheSeconds() * 1000);
    }
}

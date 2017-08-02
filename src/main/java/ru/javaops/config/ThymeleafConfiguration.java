package ru.javaops.config;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Configuration
//http://www.thymeleaf.org/doc/articles/thymeleaf3migration.html
public class ThymeleafConfiguration implements SchedulingConfigurer {
    private final Logger log = LoggerFactory.getLogger(ThymeleafConfiguration.class);

    @Autowired
    private AppProperties properties;

    @Bean
    // Attention: with TemplateEngine clear cache doesn't work
    public SpringTemplateEngine thymeleafTemplateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolvers(ImmutableSet.of(appTemplateResolver(), emailTemplateResolver()));
        return engine;
    }

    @Bean
    @Description("App template resolver")
    public ITemplateResolver appTemplateResolver() {
        log.debug("Configuring App template");
        return new FileTemplateResolver() {{
            setPrefix("./resources/view/");
            setSuffix(".html");
            setTemplateMode(TemplateMode.HTML);
            setCharacterEncoding("UTF-8");
            setOrder(1);
        }};
    }

    @Bean
    @Description("Email template resolver serving HTML 5 emails")
    public ITemplateResolver emailTemplateResolver() {
        log.debug("Configuring Email template");
        return new FileTemplateResolver() {{
            setPrefix("./resources/mails/");
            setSuffix(".html");
            setTemplateMode(TemplateMode.HTML);
            setCharacterEncoding("UTF-8");
            setOrder(2);
        }};
    }


    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedDelayTask(thymeleafTemplateEngine()::clearTemplateCache, properties.getCacheSeconds() * 2000);
    }
}

package ru.javaops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import ru.javaops.JavaOPsApplication;

/**
 * GKislin
 * 05.02.2016
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(JavaOPsApplication.class)
@WebAppConfiguration
public class JacksonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetResult() throws Exception {
        MailService.GroupResult res = new MailService.GroupResult(ImmutableList.of("a@ya.ru", "b@ya.ru"),
                ImmutableList.of(new MailService.MailResult("c@ya.ru", "fail_c"), new MailService.MailResult("d@ya.ru", "fail_d")), null);
        System.out.println(res);
        String str = objectMapper.writeValueAsString(res);
        System.out.println(str);
        Assert.assertEquals(res.toString(), objectMapper.readerFor(MailService.GroupResult.class).readValue(str).toString());
    }
}
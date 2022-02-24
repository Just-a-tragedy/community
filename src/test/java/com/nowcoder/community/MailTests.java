package com.nowcoder.community;


import com.nowcoder.community.util.MailCilent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MailTests {
    @Autowired
    private MailCilent mailCilent;
    @Test
    public void testMail(){
        mailCilent.sendMail("1169095543@qq.com","TEST","WELCOME");
    }
}

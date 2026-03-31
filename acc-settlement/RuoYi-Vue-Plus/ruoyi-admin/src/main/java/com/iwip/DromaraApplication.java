package com.iwip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 启动程序
 *
 * @author Lion Li
 */

@SpringBootApplication
public class DromaraApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DromaraApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        try {
            application.run(args);
            System.out.println("(♥◠‿◠) ﾉﾞ  RuoYi-Vue-Plus 启动成功   ლ(´ڡ`ლ)ﾞ");
        } catch (Throwable e) {
            System.err.println("RuoYi-Vue-Plus 启动失败：" + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

}

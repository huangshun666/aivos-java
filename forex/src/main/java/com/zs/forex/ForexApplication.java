package com.zs.forex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableScheduling
@EnableTransactionManagement
@SpringBootApplication
public class ForexApplication {
    public static ConfigurableApplicationContext context = null;

    public static void main(String[] args) {
        context = SpringApplication.run(ForexApplication.class, args);
    }

}

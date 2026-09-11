package com.devmetrics;

import com.devmetrics.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableScheduling
@EnableAsync
public class DevMetricsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevMetricsApplication.class, args);
    }
}

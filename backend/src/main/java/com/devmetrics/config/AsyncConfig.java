package com.devmetrics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    /**
     * Pool dedicado ao sync do GitHub: uma sincronizacao longa nunca deve
     * ocupar a thread que atende a requisicao HTTP.
     */
    @Bean(name = "githubSyncExecutor")
    public Executor githubSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("github-sync-");
        executor.initialize();
        return executor;
    }
}

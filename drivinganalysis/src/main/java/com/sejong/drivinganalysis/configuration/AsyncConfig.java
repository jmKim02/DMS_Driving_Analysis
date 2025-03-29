package com.sejong.drivinganalysis.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public Executor fileProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);     // 기본 스레드 수
        executor.setMaxPoolSize(5);      // 최대 스레드 수
        executor.setQueueCapacity(10);   // 작업 큐 용량
        executor.setThreadNamePrefix("FileProcessor-");
        executor.initialize();
        return executor;
    }
}
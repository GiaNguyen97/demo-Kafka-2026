package com.example.asyncservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    /**
     * Cau hinh thread pool cho @Async
     *
     * - corePoolSize = 10: luon co san 10 thread
     * - maxPoolSize = 50: mo rong toi da 50 thread khi tai cao
     * - queueCapacity = 100: hang doi cho khi thread pool day
     *
     * => Day chinh la gioi han cua @Async:
     *    Spam 500 req qua nhanh → queue full → RejectedExecutionException
     *    (Kafka khong bi van de nay vi message nam trong broker)
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AsyncThread-");
        executor.initialize();
        return executor;
    }
}

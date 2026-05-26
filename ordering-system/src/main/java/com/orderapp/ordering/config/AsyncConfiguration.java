package com.orderapp.ordering.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async/Concurrency Configuration
 * 
 * Abilita @Async su service methods
 * Configura thread pool per operazioni non-blocking
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core threads: numero di thread sempre attivi
        executor.setCorePoolSize(5);
        
        // Max threads: numero massimo di thread
        executor.setMaxPoolSize(20);
        
        // Queue capacity: numero di task in coda di attesa
        executor.setQueueCapacity(100);
        
        // Thread name prefix per debugging
        executor.setThreadNamePrefix("async-task-");
        
        // Wait for tasks to complete at shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // Timeout di attesa in secondi
        executor.setAwaitTerminationSeconds(60);
        
        // Rejection policy: cosa fare quando la queue è piena
        executor.setRejectedExecutionHandler(
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        executor.initialize();
        return executor;
    }
}

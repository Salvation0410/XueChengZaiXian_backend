package com.xuecheng.messagesdk.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @description message 模块线程池配置
 */
@Configuration
public class MessageThreadPoolConfig {

    @Bean(value = "messageProcessThreadPool", destroyMethod = "shutdown")
    public ExecutorService messageProcessThreadPool() {
        int processors = Runtime.getRuntime().availableProcessors();
        int corePoolSize = processors * 2 + 1;
        int maximumPoolSize = processors * 2 + 1;
        return new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("message-process-pool-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}

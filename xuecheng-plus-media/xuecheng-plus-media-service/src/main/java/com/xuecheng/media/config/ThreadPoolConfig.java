package com.xuecheng.media.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName ThreadPoolConfig
 * @Description 线程池配置类 全局公用一个线程池 防止重复创建线程池
 * @Author huang
 * @Date 2025/9/5
 */


@Configuration
public class ThreadPoolConfig {

    @Bean("mediaProcessThreadPool")
    public ExecutorService mediaProcessThreadPool() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,  //核心线程数
                corePoolSize, //最大线程数
                60L, //空闲线程存活时间
                TimeUnit.SECONDS, //时间单位->秒
                new LinkedBlockingQueue<>(100),//工作队列（容量100）
                new ThreadFactoryBuilder().setNameFormat("media-process-pool-%d").build(),//线程工厂->自定义线程命名格式
                new ThreadPoolExecutor.CallerRunsPolicy()//拒绝策略
        );

        // 注册ShutdownHook确保优雅关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdown();        // 1. 停止接收新任务
            try {
                // 2. 等待60秒让现有任务完成
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // 3. 如果超时，强制终止
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();    // 4. 被中断时强制终止
                Thread.currentThread().interrupt();
            }
        }));

        return executor;
    }
}

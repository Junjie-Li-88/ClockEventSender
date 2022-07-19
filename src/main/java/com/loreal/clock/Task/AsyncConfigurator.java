package com.loreal.clock.Task;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
// ThredPoolTaskExcutor的处理流程
// 当池子大小小于corePoolSize，就新建线程，并处理请求
// 当池子大小等于corePoolSize，把请求放入workQueue中，池子里的空闲线程就去workQueue中取任务并处理
// 当workQueue放不下任务时，就新建线程入池，并处理请求，如果池子大小撑到了maximumPoolSize，就用RejectedExecutionHandler来做拒绝处理
// 当池子的线程数大于corePoolSize时，多余的线程会等待keepAliveTime长时间，如果无请求可处理就自行销毁
@Configuration
@EnableAsync
public class AsyncConfigurator implements AsyncConfigurer {
@Bean("doSomethingExecutor")
public Executor doSomethingExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    // 核心线程数：线程池创建时候初始化的线程数
    executor.setCorePoolSize(10);
    // 最大线程数：线程池最大的线程数，只有在缓冲队列满了之后才会申请超过核心线程数的线程
    executor.setMaxPoolSize(20);
    // 缓冲队列：用来缓冲执行任务的队列
    executor.setQueueCapacity(500);
    // 允许线程的空闲时间60秒：当超过了核心线程之外的线程在空闲时间到达之后会被销毁
    executor.setKeepAliveSeconds(60);
    // 线程池名的前缀：设置好了之后可以方便我们定位处理任务所在的线程池
    executor.setThreadNamePrefix("do-something-");
    // 缓冲队列满了之后的拒绝策略：由调用线程处理（一般是主线程）
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
    executor.initialize();
    return executor;
}
}

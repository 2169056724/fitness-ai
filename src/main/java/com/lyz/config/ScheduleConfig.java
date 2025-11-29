package com.lyz.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 定时任务配置类
 * 配置定时任务的线程池，避免任务阻塞
 */
@Configuration
public class ScheduleConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        // 设置线程池大小（可以并发执行多个定时任务）
        taskScheduler.setPoolSize(5);
        // 设置线程名前缀
        taskScheduler.setThreadNamePrefix("scheduled-task-");
        // 设置线程池关闭时等待所有任务完成
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        // 设置等待时间（秒）
        taskScheduler.setAwaitTerminationSeconds(60);
        taskScheduler.initialize();
        
        taskRegistrar.setTaskScheduler(taskScheduler);
    }
}

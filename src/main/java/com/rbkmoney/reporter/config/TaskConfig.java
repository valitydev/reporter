package com.rbkmoney.reporter.config;

import com.rbkmoney.reporter.factory.AutowiringSpringBeanJobFactory;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by tolkonepiu on 17/07/2017.
 */
@Configuration
public class TaskConfig {

    @Bean
    @DependsOn("dataSource")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setDaemon(true);
        ThreadGroup threadGroup = new ThreadGroup("Schedulers");
        taskScheduler.setThreadFactory(new ThreadFactory() {
            AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(threadGroup, r, "Scheduler-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        });
        return taskScheduler;
    }

    @Bean
    @DependsOn("flyway")
    public SchedulerFactoryBean schedulerFactory(ApplicationContext applicationContext,
                                                 DataSource dataSource,
                                                 QuartzProperties quartzProperties) {
        SchedulerFactoryBean factoryBean = new SchedulerFactoryBean();
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);

        factoryBean.setJobFactory(jobFactory);
        factoryBean.setApplicationContextSchedulerContextKey("applicationContext");
        factoryBean.setOverwriteExistingJobs(true);
        factoryBean.setDataSource(dataSource);

        final Properties properties = new Properties();
        properties.putAll(quartzProperties.getProperties());
        factoryBean.setQuartzProperties(properties);

        return factoryBean;
    }

}

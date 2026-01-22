package com.ktk.workhuservice;

import com.ktk.workhuservice.config.MicrosoftConfig;
import com.ktk.workhuservice.service.UserImportService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({MicrosoftConfig.class})
public class WorkHuServiceApplication {

    public WorkHuServiceApplication(UserImportService userImportService) {
        userImportService.importUsersFromCsv();
    }

    public static void main(String[] args) {
        System.out.println("--> Current working directory: " + System.getProperty("user.dir"));
        SpringApplication.run(WorkHuServiceApplication.class, args);
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("Async-");
        threadPoolTaskExecutor.setCorePoolSize(4);
        threadPoolTaskExecutor.setMaxPoolSize(6);
        threadPoolTaskExecutor.setQueueCapacity(5);
        return threadPoolTaskExecutor;
    }
}

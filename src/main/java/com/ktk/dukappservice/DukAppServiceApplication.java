package com.ktk.dukappservice;

import com.ktk.dukappservice.config.MicrosoftConfig;
import com.ktk.dukappservice.service.UserImportService;
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
public class DukAppServiceApplication {

    public DukAppServiceApplication(UserImportService userImportService) {
        userImportService.importUsersFromCsv();
    }

    public static void main(String[] args) {
        System.out.println("--> Current working directory: " + System.getProperty("user.dir"));
        SpringApplication.run(DukAppServiceApplication.class, args);
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

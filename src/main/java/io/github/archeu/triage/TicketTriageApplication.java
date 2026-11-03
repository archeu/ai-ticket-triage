package io.github.archeu.triage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan // 读取 config.TriageProperties（application.properties 里 triage. 开头的配置）
@EnableScheduling            // 开启定时任务（job.TicketCleanupJob 用）
public class TicketTriageApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketTriageApplication.class, args);
    }
}

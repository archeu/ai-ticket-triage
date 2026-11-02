package com.example.triage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 开启定时任务（TicketCleanup 用）
public class TicketTriageApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketTriageApplication.class, args);
	}

}

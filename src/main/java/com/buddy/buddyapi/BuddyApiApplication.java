package com.buddy.buddyapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class BuddyApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BuddyApiApplication.class, args);
	}

}

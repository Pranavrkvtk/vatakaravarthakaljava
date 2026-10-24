package com.example.newsjava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NewsjavaApplication {

	public static void main(String[] args) {
		SpringApplication.run(NewsjavaApplication.class, args);
	}

}
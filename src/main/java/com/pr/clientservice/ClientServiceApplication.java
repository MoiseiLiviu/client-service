package com.pr.clientservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClientServiceApplication {

	public static final Long TIME_UNIT = 50L;

	public static void main(String[] args) {
		SpringApplication.run(ClientServiceApplication.class, args);
	}

}

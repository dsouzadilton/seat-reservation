package com.dilton.paytm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PaytmApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaytmApplication.class, args);
	}

}

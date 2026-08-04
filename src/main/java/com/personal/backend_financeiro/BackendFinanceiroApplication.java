package com.personal.backend_financeiro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BackendFinanceiroApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendFinanceiroApplication.class, args);
	}

}

package io.dougluciano.poc.ai.openjarvis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class OpenjarvisApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenjarvisApplication.class, args);
	}

}

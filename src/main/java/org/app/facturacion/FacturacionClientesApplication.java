package org.app.facturacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Instant;

@EnableAsync
@SpringBootApplication
public class FacturacionClientesApplication {

	private static final Instant startTime = Instant.now();

	public static void main(String[] args) {
		SpringApplication.run(FacturacionClientesApplication.class, args);
	}

	@Bean
	public Instant startTime() {
		return startTime;
	}

}

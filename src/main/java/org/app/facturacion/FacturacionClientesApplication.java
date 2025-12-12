package org.app.facturacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class FacturacionClientesApplication {

	public static void main(String[] args) {
		SpringApplication.run(FacturacionClientesApplication.class, args);
	}

}

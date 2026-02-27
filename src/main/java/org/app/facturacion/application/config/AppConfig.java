package org.app.facturacion.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
  /**
   * Define un Bean de RestTemplate para ser usado en la inyección de
   * dependencias.
   */
  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

}
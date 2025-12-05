package org.app.facturacion.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
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

  /**
   * Define el Bean de JavaMailSender.
   */
  @Bean
  public JavaMailSender javaMailSender() {
    return new JavaMailSenderImpl();
  }

}
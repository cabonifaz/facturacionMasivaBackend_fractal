package org.app.facturacion.infrastructure.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(5); // Hilos siempre activos
    executor.setMaxPoolSize(10); // Máximo de hilos si la cola se llena
    executor.setQueueCapacity(100); // Tareas en espera antes de crear más hilos
    executor.setThreadNamePrefix("FacturacionAsync-");
    executor.initialize();
    return executor;
  }
}

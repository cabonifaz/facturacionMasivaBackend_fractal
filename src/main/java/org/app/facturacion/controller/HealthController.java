package org.app.facturacion.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@SuppressWarnings("null")
@RestController
@RequestMapping("/health")
public class HealthController {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private Instant startTime;

  @GetMapping
  public ResponseEntity<Object> checkHealth() {
    String dbStatus = checkDatabase();
    String uptime = calculateUptime();
    String memoryUsage = this.getMemoryUsage();

    record CheckResponse(
        String status,
        String uptime,
        String dbStatus,
        String memoryUsage) {
    }

    var response = new CheckResponse(
        "OK",
        uptime,
        dbStatus,
        memoryUsage);

    return ResponseEntity.ok(response);
  }

  private String checkDatabase() {
    try {
      jdbcTemplate.queryForObject("SELECT 1", Integer.class);
      return "CONNECTED";
    } catch (Exception e) {
      return "DISCONNECTED";
    }
  }

  private String calculateUptime() {
    Duration duration = Duration.between(startTime, Instant.now());
    long hours = duration.toHours();
    long minutes = duration.toMinutesPart();
    long seconds = duration.toSecondsPart();
    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
  }

  private String getMemoryUsage() {
    Runtime runtime = Runtime.getRuntime();
    long totalMemory = runtime.totalMemory() / (1024 * 1024);
    long freeMemory = runtime.freeMemory() / (1024 * 1024);
    long usedMemory = totalMemory - freeMemory;
    long maxMemory = runtime.maxMemory() / (1024 * 1024);

    return String.format("Used: %dMB | Reserved: %dMB | Max: %dMB",
        usedMemory, totalMemory, maxMemory);
  }

}

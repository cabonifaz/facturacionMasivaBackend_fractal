package org.app.facturacion.infrastructure.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
  @GetMapping("/")
  public String home() {
    return "API FactClientBk is working version 0.0.1";
  }
}

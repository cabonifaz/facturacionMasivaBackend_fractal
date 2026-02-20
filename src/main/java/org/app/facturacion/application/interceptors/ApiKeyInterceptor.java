package org.app.facturacion.application.interceptors;

import org.app.facturacion.domain.exceptions.AuthorizationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

  @Value("${app.api.key}")
  private String apiKey;

  @Override
  public boolean preHandle(HttpServletRequest request,
      HttpServletResponse response,
      Object handler) {

    // Ignore OPTIONS requests for CORS
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }

    String requestApiKey = request.getHeader("XFR-API-KEY");

    // Allow requests that have the API Key
    if (apiKey == null || apiKey.isEmpty()) {
      return true;
    }

    if (requestApiKey == null || !requestApiKey.equals(apiKey)) {
      throw new AuthorizationException("API Key inválida o faltante");
    }

    return true;
  }

}

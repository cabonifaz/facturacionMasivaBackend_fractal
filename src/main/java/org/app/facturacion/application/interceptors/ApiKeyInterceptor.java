package org.app.facturacion.application.interceptors;

import org.app.facturacion.domain.exceptions.AuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

  private final Logger logger = LoggerFactory.getLogger(ApiKeyInterceptor.class);

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
      this.logger.info("CORS allowed by XFR-API-KEY");
      return true;
    }

    // Allow health check
    if ("/health".equals(request.getRequestURI())) {
      this.logger.info("Health check allowed by XFR-API-KEY");
      return true;
    }

    if (requestApiKey == null || !requestApiKey.equals(apiKey)) {
      this.logger.error("Request blocked! by API KEY Interceptor");
      throw new AuthorizationException("API Key inválida o faltante");
    }

    return true;
  }

}

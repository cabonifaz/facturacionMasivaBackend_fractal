package org.app.facturacion.infrastructure.controller;

import org.app.facturacion.domain.exceptions.SystemAPIException;
import org.app.facturacion.domain.exceptions.ValidationAPIException;
import org.app.facturacion.domain.models.BaseAPIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @SuppressWarnings("null")
  @ExceptionHandler(SystemAPIException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public BaseAPIResponse<?> handleSystemException(SystemAPIException e) {
    String msg = e.getMessage() == null ? "No se determino el motivo del error" : e.getMessage();
    return BaseAPIResponse.error(msg);
  }

  @SuppressWarnings("null")
  @ExceptionHandler(ValidationAPIException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public BaseAPIResponse<?> handleValidationException(ValidationAPIException e) {
    String msg = e.getMessage() == null ? "No se determino el motivo del error" : e.getMessage();
    return BaseAPIResponse.error(msg);
  }

  @SuppressWarnings("null")
  @ExceptionHandler(ResponseStatusException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public BaseAPIResponse<?> handleResponseStatus(ResponseStatusException e) {
    String msg = e.getMessage() == null ? "No se determino el motivo del error" : e.getMessage();
    logger.warn("Bad Request: ", msg);
    return BaseAPIResponse.error(msg);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public BaseAPIResponse<?> handleGenericException(Exception e) {
    logger.error("Unexpected error: ", e);
    return BaseAPIResponse.error("Error inesperado en el servidor");
  }
}

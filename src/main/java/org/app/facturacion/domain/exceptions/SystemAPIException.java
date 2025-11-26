package org.app.facturacion.domain.exceptions;

public class SystemAPIException extends RuntimeException {
  public SystemAPIException(String message, Throwable cause) {
    super(message, cause);
  }
}
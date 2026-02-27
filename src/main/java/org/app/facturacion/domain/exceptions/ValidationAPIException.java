package org.app.facturacion.domain.exceptions;

public class ValidationAPIException extends RuntimeException {
  public ValidationAPIException(String message) {
    super(message);
  }
}

package org.app.facturacion.domain.exceptions;

public class BusinessAPIException extends RuntimeException {
  public BusinessAPIException(String message) {
    super(message);
  }
}

package org.app.facturacion.domain.exceptions;

public class AuthorizationException extends RuntimeException {

  public AuthorizationException(String message) {
    super(message);
  }
}

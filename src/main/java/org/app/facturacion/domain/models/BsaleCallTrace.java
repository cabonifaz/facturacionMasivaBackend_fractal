package org.app.facturacion.domain.models;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BsaleCallTrace {
  private final Object input;
  private final Object output;
}

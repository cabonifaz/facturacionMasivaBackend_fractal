package org.app.facturacion.domain.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class BaseAPIResponse<T> {

  @Nonnull
  private String message;

  @Nullable
  private T data;
}

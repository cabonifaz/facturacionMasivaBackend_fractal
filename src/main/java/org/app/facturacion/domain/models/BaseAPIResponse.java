package org.app.facturacion.domain.models;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class BaseAPIResponse<T> {

  private final ApiStatus status;

  @NonNull
  private final String message;

  @Nullable
  private final T data;

  @Nullable
  private final List<String> warnings;

  public static <T> BaseAPIResponse<T> success(@NonNull String message, T data) {
    return new BaseAPIResponse<>(ApiStatus.SUCCESS, message, data, null);
  }

  public static <T> BaseAPIResponse<T> warning(@NonNull String message, T data, List<String> warnings) {
    return new BaseAPIResponse<>(ApiStatus.WARNING, message, data, warnings);
  }

  public static <T> BaseAPIResponse<T> error(@NonNull String message) {
    return new BaseAPIResponse<>(ApiStatus.ERROR, message, null, null);
  }

}

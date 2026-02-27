package org.app.facturacion.domain.models;

import org.eclipse.jdt.annotation.NonNull;

import jakarta.annotation.Nonnull;
import lombok.Data;

@Data
public class Workload {

  @NonNull
  @Nonnull
  String workloadId;

}

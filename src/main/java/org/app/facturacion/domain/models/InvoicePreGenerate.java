package org.app.facturacion.domain.models;

import jakarta.annotation.Nonnull;
import lombok.Data;

@Data
public class InvoicePreGenerate {

  @Nonnull
  private String workload;

  @Nonnull
  private String code;

  private String address;
  private String district;
  private String city;
  private String company;
  private String activity;
  private String bankAccount;
  private String paymentMethod;
  private Integer paymentId;
  private String operationType;

}

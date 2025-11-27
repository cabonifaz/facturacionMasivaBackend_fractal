package org.app.facturacion.domain.models;

import lombok.Data;

@Data
public class InvoiceHistoryDetails {
  private Integer id;
  private Integer orderNumber;
  private String concept;
  private Integer incomingNumber;
  private Double subTotal;
  private Double amountPerUnit;
  private Double discount;
  private Integer quantity;
}

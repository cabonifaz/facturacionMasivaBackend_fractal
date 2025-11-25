package org.app.facturacion.domain.models;

import lombok.Data;

@Data
public class InvoiceRow {
  private String clientName;
  private String analytic;
  private String ocOs;
  private Integer niCs;
  private String collaborator;
  private String startDate;
  private String endDate;
  private String concept;
  private String currency;
  private Double amount;
  private Double igv;
  private Double totalAmount;
  private String contact;
  private String invoiceNumber;
}

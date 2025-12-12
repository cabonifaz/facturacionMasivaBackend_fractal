package org.app.facturacion.domain.models;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvoicesTableReport {

  private String clientName;
  private String incommingNumber;
  private String orderNumber;
  private String startDate;
  private String endDate;
  private String concept;
  private String analytic;

  private String currencyName;
  private BigDecimal pricePerUnit;
  private BigDecimal igv;
  private BigDecimal totalToPay;

  private String contact;
  private String invoiceNumber;
  private Long documentId;
  private String collaborator;
  private String observation;
}
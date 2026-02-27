package org.app.facturacion.domain.models;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Builder
@Data
public class InvoicesTableReport {
  private Integer incomingNumber;
  private String startDate;
  private String endDate;
  private String concept;
  private BigDecimal pricePerUnit;
  private String clientName;
  private String analytic;
  private BigDecimal igv;
  private BigDecimal totalToPay;
  private String contact;
  private String invoiceNumber;
  private String currencyName;
  private String collaborator;
  private String observation;
}
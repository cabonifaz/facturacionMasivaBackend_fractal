package org.app.facturacion.domain.models;

import java.util.List;

import lombok.Data;

@Data
public class InvoiceHeader {

  private Integer historyId;
  private String clientAddress;
  private String clientDistrict;
  private String clientCity;
  private String clientCode;
  private String workload;
  private String clientActivity;
  private String clientProvince;
  private String observation;
  private Integer orderNumber;
  private Integer state;

  private List<InvoiceHistoryDetails> details;

}

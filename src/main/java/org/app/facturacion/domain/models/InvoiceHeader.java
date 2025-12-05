package org.app.facturacion.domain.models;

import java.util.List;
import lombok.Data;

@Data
public class InvoiceHeader {

  private Integer historyId;
  private String workload;
  private Integer orderNumber;

  private Integer incomingNumber;

  private String observation;
  private String clientCode;
  private String clientAddress;
  private String clientDistrict;
  private String clientCity;
  private String clientProvince;
  private String clientActivity;
  private Integer state;

  private Double detractionAmount;
  private Double totalToPay;

  private List<InvoiceDetail> details;

  /**
   * Clase interna estática para mapear los detalles que vienen dentro del JSON
   */
  @Data
  public static class InvoiceDetail {
    private Integer orderNumber;
    private String concept;
    private Integer incomingNumber;
    private Double subTotal;
    private Double amountPerUnit;
    private Integer quantity;
    private Double discount;
  }
}
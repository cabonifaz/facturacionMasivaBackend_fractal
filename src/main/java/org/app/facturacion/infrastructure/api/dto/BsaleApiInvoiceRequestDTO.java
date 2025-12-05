package org.app.facturacion.infrastructure.api.dto;

import java.util.List;

import org.app.facturacion.domain.models.InvoiceHistoryDetails;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BsaleApiInvoiceRequestDTO {

  private String code;
  private String observation;
  private String address;
  private String district;
  private String city;
  private String province;

  private String company;
  private String activity;

  private List<InvoiceHistoryDetails> details;

}
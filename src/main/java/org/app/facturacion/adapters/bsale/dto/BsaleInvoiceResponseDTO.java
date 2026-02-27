package org.app.facturacion.adapters.bsale.dto;

import lombok.Data;

@Data
public class BsaleInvoiceResponseDTO {

  private Long id;

  private String number;
  private String urlPublicView;
  private String token;
  private String serialNumber;
  private Boolean status;
  private String urlPublicViewOriginal;
  private String urlPdfOriginal;
}

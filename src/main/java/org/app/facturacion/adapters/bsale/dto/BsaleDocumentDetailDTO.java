package org.app.facturacion.adapters.bsale.dto;

import org.eclipse.jdt.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BsaleDocumentDetailDTO {

  private Long id;

  @NonNull
  private String serialNumber;

  @NonNull
  private String urlPdfOriginal;
}

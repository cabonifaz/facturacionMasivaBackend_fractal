package org.app.facturacion.domain.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FIleModelDTO {

  private String filename;
  private byte[] fileBytes;
  private String fileType;

}

package org.app.facturacion.domain.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileModelDTO {

  private String filename;
  private byte[] fileBytes;
  private String fileType;

  @Override
  public String toString() {

    return new StringBuilder()
        .append("Filename: ")
        .append(this.filename)
        .append(" ")
        .append("Size: ")
        .append(fileBytes.length)
        .append(" bytes")
        .append(" ")
        .append("MediaType: ")
        .append(this.fileType)
        .toString();
  }

}

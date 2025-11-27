package org.app.facturacion.infrastructure.api.dto;

import java.util.List;

import org.app.facturacion.domain.models.InvoiceHistoryDetails;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BsaleApiInvoiceRequestDTO {

  private final Integer documentTypeId = 85;

  private String code; // Mapear el RUC del cliente
  private String observation;
  private String address; // Mapea a DIRECCION_CLIENTE

  private String district; // Mapea a DISTRITO_CLIENTE

  private String city; // Mapea a CIUDAD_CLIENTE

  private String company; // Mapea a NOMBRE_CLIENTE

  private String activity; // Mapea a ACTIVIDAD_CLIENTE

  private String bankAccount; // Mapea a CUENTA_BANCO

  private String paymentMethod; // Mapea a TIPO_PAGO

  private Integer paymentId; // Mapea a ID_TIPO_PAGO

  private List<InvoiceHistoryDetails> details;

}
package org.app.facturacion.application.services;

import java.util.List;

import org.app.facturacion.domain.exceptions.ValidationAPIException;
import org.app.facturacion.domain.models.BaseAPIResponse;
import org.app.facturacion.domain.models.InvoiceRow;
import org.app.facturacion.domain.port.InvoiceBatchRepositoryPort;
import org.app.facturacion.infrastructure.mappers.ExcelReader;
import org.app.facturacion.infrastructure.repositories.InvoiceBatchRepository;
import org.eclipse.jdt.annotation.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

@Service
public class InvoiceBatchService {

  private final InvoiceBatchRepositoryPort repository;

  public InvoiceBatchService(InvoiceBatchRepository repo) {
    this.repository = repo;
  }

  /**
   * Procesa la carga de trabajo
   * 
   * @param file Archivo a procesar
   * @return retorna el código de carga de los datos procesados
   */
  public BaseAPIResponse<String> processInvoceBatchFile(@NonNull MultipartFile file) {

    ExcelReader reader = new ExcelReader();
    List<InvoiceRow> invoices = reader.readInvoiceSheet(file);

    // Guardar en la base de datos
    String workLoadId = this.repository.addOrUpdateInvoiceWorkspace(invoices, "system-user");

    return BaseAPIResponse.success("Datos procesados correctamente", workLoadId);

  }

  /**
   * Genera el detalle de facturación para una carga de trabajo
   * 
   * @param workloadId ID de la carga de trabajo
   * @return Retorna true si se generan los detalles para la carga de trabajo
   */
  @PostMapping("create-details")
  public BaseAPIResponse<Boolean> createDetailsForWorkLoad(String workloadId) {

    if (workloadId == null)
      throw new ValidationAPIException("El ID de la carga de trabajo no puede ser nulo");

    Boolean response = this.repository.createDetailsForWorkload(workloadId, "system-user");

    return BaseAPIResponse.success("Detalles generados correctamente", response);
  }

}

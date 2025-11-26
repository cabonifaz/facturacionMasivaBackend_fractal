package org.app.facturacion.application.services;

import java.util.List;

import org.app.facturacion.domain.models.BaseAPIResponse;
import org.app.facturacion.domain.models.InvoiceRow;
import org.app.facturacion.domain.port.InvoiceBatchRepositoryPort;
import org.app.facturacion.infrastructure.mappers.ExcelReader;
import org.app.facturacion.infrastructure.repositories.InvoiceBatchRepository;
import org.eclipse.jdt.annotation.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class InvoiceBatchService {

  private final InvoiceBatchRepositoryPort repository;

  public InvoiceBatchService(InvoiceBatchRepository repo) {
    this.repository = repo;
  }

  /**
   * @return retorna el código de carga de los datos procesados
   */
  public BaseAPIResponse<String> processInvoceBatchFile(@NonNull MultipartFile file) {

    ExcelReader reader = new ExcelReader();
    List<InvoiceRow> invoices = reader.readInvoiceSheet(file);

    // Guardar en la base de datos
    String workLoadId = this.repository.addOrUpdateInvoiceWorkspace(invoices, "system-user");

    return BaseAPIResponse.success("Datos procesados correctamente", workLoadId);

  }
}

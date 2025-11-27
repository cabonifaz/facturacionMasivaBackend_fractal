package org.app.facturacion.application.services;

import java.util.List;

import org.app.facturacion.domain.exceptions.ValidationAPIException;
import org.app.facturacion.domain.models.BaseAPIResponse;
import org.app.facturacion.domain.models.InvoiceHistory;
import org.app.facturacion.domain.models.InvoicePreGenerate;
import org.app.facturacion.domain.models.InvoiceRow;
import org.app.facturacion.domain.port.InvoiceBatchRepositoryPort;
import org.app.facturacion.domain.port.InvoiceHistoryRepositoryPort;
import org.app.facturacion.infrastructure.api.adapter.BsaleApiAdapter;
import org.app.facturacion.infrastructure.api.dto.BsaleApiInvoiceRequestDTO;
import org.app.facturacion.infrastructure.api.dto.BsaleInvoiceResponseDTO;
import org.app.facturacion.infrastructure.mappers.ExcelReader;
import org.app.facturacion.infrastructure.repositories.InvoiceBatchRepository;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class InvoiceBatchService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final InvoiceBatchRepositoryPort repository;
  private final BsaleApiAdapter bsaleApiAdapter;
  private final InvoiceHistoryRepositoryPort invoiceHisRp;

  public InvoiceBatchService(InvoiceBatchRepository repo, BsaleApiAdapter adapter, InvoiceHistoryRepositoryPort rp) {
    this.repository = repo;
    this.bsaleApiAdapter = adapter;
    this.invoiceHisRp = rp;
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
  public BaseAPIResponse<Boolean> createDetailsForWorkLoad(String workloadId) {

    if (workloadId == null)
      throw new ValidationAPIException("El ID de la carga de trabajo no puede ser nulo");

    Boolean response = this.repository.createDetailsForWorkload(workloadId, "system-user");

    return BaseAPIResponse.success("Detalles generados correctamente", response);
  }

  /**
   * Pregenera los datos necesarios: Agrupacion por Notas de ingreso, cálculo de
   * montos, etc.
   * 
   * @return Retorna true si el proceso se completa correctamente
   */
  public BaseAPIResponse<Boolean> pregenerateHeaders(@NonNull InvoicePreGenerate rGenerate) {

    Boolean response = this.repository.pregenerateInvoices(rGenerate, "system-user");

    return BaseAPIResponse.success("Datos pregenerados correctamente", response);
  }

  @SuppressWarnings("null")
  public BaseAPIResponse<String> generateInvoices(@NonNull String workload) {

    List<InvoiceHistory> invoices = this.invoiceHisRp.findPendingInvoicesByWorkload(workload);

    if (invoices.isEmpty()) {
      this.logger.warn("Not pending invoices for: {}", workload);
      return BaseAPIResponse.error("No se encontraron facturas pendientes para el código de carga: " + workload);
    }

    this.logger.info("Pending invoices: {}", invoices.size());

    int successfulCount = 0;

    for (InvoiceHistory invoice : invoices) {
      try {
        BsaleApiInvoiceRequestDTO request = mapToApiRequest(invoice);
        this.logger.debug("Request to Bsale: {}", request);
        this.logger.debug("Details size: {}", request.getDetails().size());
        this.logger.debug("Details: {}", request.getDetails());

        BsaleInvoiceResponseDTO response = bsaleApiAdapter.createExternalInvoice(request);

        if (response != null && response.getId() != null) {

          invoiceHisRp.updateInvoiceStatus(
              invoice.getHistoryId(),
              response.getSerialNumber(),
              response.getId());
          successfulCount++;
        }
      } catch (Exception e) {
        this.logger.error("Error processing Invoice ID " + invoice.getHistoryId() +
            ": " + e.getMessage());
      }
    }

    String message = String.format("Proceso completado. %d de %d facturas enviadas exitosamente.",
        successfulCount, invoices.size());

    this.logger.info("Invoices generated: {}", invoices.size());

    return BaseAPIResponse.success(message, message);
  }

  private @NonNull BsaleApiInvoiceRequestDTO mapToApiRequest(InvoiceHistory invoice) {
    BsaleApiInvoiceRequestDTO dto = new BsaleApiInvoiceRequestDTO();
    dto.setCode(invoice.getClientCode());
    dto.setAddress(invoice.getClientAddress());
    dto.setDistrict(invoice.getClientDistrict());
    dto.setCity(invoice.getClientCity());
    dto.setCompany("");
    dto.setActivity(invoice.getClientActivity());
    dto.setBankAccount("");
    dto.setObservation(invoice.getObservation());
    // dto.setPaymentMethod(invoice.getTipoPago());
    dto.setPaymentId(1);
    dto.setDetails(invoice.getDetails());
    return dto;
  }

}

package org.app.facturacion.application.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.app.facturacion.application.utilities.ExcelHelper;
import org.app.facturacion.domain.exceptions.SystemAPIException;
import org.app.facturacion.domain.exceptions.ValidationAPIException;
import org.app.facturacion.domain.models.BaseAPIResponse;
import org.app.facturacion.domain.models.FileModelDTO;
import org.app.facturacion.domain.models.InvoiceHeader;
import org.app.facturacion.domain.models.InvoicePreGenerate;
import org.app.facturacion.domain.models.InvoiceRow;
import org.app.facturacion.domain.port.InvoiceBatchRepositoryPort;
import org.app.facturacion.domain.port.InvoiceHistoryRepositoryPort;
import org.app.facturacion.infrastructure.api.adapter.BsaleApiAdapter;
import org.app.facturacion.infrastructure.api.adapter.N8NAdapter;
import org.app.facturacion.infrastructure.api.dto.BsaleInvoiceResponseDTO;
import org.app.facturacion.infrastructure.mappers.ExcelReader;
import org.app.facturacion.infrastructure.repositories.InvoiceBatchRepository;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class InvoiceService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final InvoiceBatchRepositoryPort repository;
  private final BsaleApiAdapter bsaleApiAdapter;
  private final InvoiceHistoryRepositoryPort invoiceHisRp;
  private final N8NAdapter n8nAdapter;

  public InvoiceService(InvoiceBatchRepository repo, BsaleApiAdapter adapter, InvoiceHistoryRepositoryPort rp,
      N8NAdapter n8nAdapter,
      EmailService emailService) {
    this.repository = repo;
    this.bsaleApiAdapter = adapter;
    this.invoiceHisRp = rp;
    this.n8nAdapter = n8nAdapter;
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
  public BaseAPIResponse<byte[]> generateInvoices(@NonNull String workload) {

    List<InvoiceHeader> invoices = this.invoiceHisRp.findPendingInvoicesByWorkload(workload);

    if (invoices.isEmpty()) {
      this.logger.warn("Not pending invoices for: {}", workload);
      return BaseAPIResponse.error("No se encontraron facturas pendientes para el código de carga: " + workload);
    }

    this.logger.info("Pending invoices: {}", invoices.size());

    Integer successfulCount = 0;
    record InvoiceDoc(String documentName, String documentUrl) {
    }
    List<InvoiceDoc> generatedDocs = new ArrayList<>();

    for (InvoiceHeader invoice : invoices) {
      try {

        this.logger.info("Invoice for-> OS/OC: {} and NI: {}", invoice.getObservation(), invoice.getIncomingNumber());
        BsaleInvoiceResponseDTO response = bsaleApiAdapter.createExternalInvoice(invoice);

        if (response != null && response.getId() != null) {
          generatedDocs.add(new InvoiceDoc(response.getSerialNumber(), response.getUrlPdfOriginal()));

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

    this.logger.info(message);
    this.logger.info("Invoices generated: {}", invoices.size());
    this.logger.info("Downloading {} files", successfulCount);

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos)) {

      for (InvoiceDoc doc : generatedDocs) {
        try {
          byte[] pdfBytes = this.bsaleApiAdapter.downloadBsaleDocument(doc.documentUrl());

          if (pdfBytes != null && pdfBytes.length > 0) {
            String safeFileName = doc.documentName().replaceAll("[\\\\/:*?\"<>|]", "_") + ".pdf";

            ZipEntry entry = new ZipEntry(safeFileName);
            zos.putNextEntry(entry);
            zos.write(pdfBytes);
            zos.closeEntry();
          }
        } catch (Exception e) {
          this.logger.error("Error downloading/zipping invoice " + doc.documentName(), e);
        }
      }

      zos.finish();
      byte[] zipBytes = baos.toByteArray();

      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));

      var zipFileName = new StringBuilder()
          .append(timestamp)
          .append("_")
          .append("reporte_facturas.zip")
          .toString();

      this.logger.info("ZIP generated successfully. Size: {} bytes", zipBytes.length);
      this.n8nAdapter.callCreateExcelHook(workload);
      return BaseAPIResponse.success(zipFileName, zipBytes);
    } catch (Exception e) {
      this.logger.error("Error generating final ZIP", e);
      return BaseAPIResponse.warning("Se co", null, null);
    }
  }

  public FileModelDTO generateTableReport(@NonNull String workload) {

    this.logger.info("Generating table report for: {}", workload);
    var reportData = this.repository.getTableReportByWorkload(workload);
    this.logger.info("Data received from BD: {}", reportData.size());

    if (reportData.isEmpty())
      throw new SystemAPIException(
          "No hay facturas completadas para generar reporte",
          null);

    try {
      this.logger.info("Generating Excel bytes");
      var excelBytes = ExcelHelper.generateInvoiceReport(reportData);
      this.logger.info("File size: {} bytes", excelBytes.length);

      var formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm");
      var timestamp = LocalDateTime.now().format(formatter);
      var filename = String.format("%s reporte_facturacion.xlsx", timestamp);

      var fileDto = FileModelDTO
          .builder()
          .fileBytes(excelBytes)
          .filename(filename)
          .fileType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
          .build();

      this.logger.info("File model built: {}", fileDto.toString());

      return fileDto;
    } catch (IOException e) {
      throw new SystemAPIException("Ocurrió un error generando el excel de reportes", e.getCause());
    }

  }

}
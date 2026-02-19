package org.app.facturacion.application.services;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.app.facturacion.application.utilities.ExcelHelper;
import org.app.facturacion.application.utilities.ZipSysHelper;
import org.app.facturacion.domain.exceptions.SystemAPIException;
import org.app.facturacion.domain.exceptions.ValidationAPIException;
import org.app.facturacion.domain.models.BaseAPIResponse;
import org.app.facturacion.domain.models.FileModelDTO;
import org.app.facturacion.domain.models.InvoiceHeader;
import org.app.facturacion.domain.models.InvoiceRow;
import org.app.facturacion.domain.models.Workload;
import org.app.facturacion.domain.port.InvoiceBatchRepositoryPort;
import org.app.facturacion.domain.port.InvoiceHistoryRepositoryPort;
import org.app.facturacion.infrastructure.api.adapter.BsaleApiAdapter;
import org.app.facturacion.infrastructure.mappers.ExcelReader;
import org.app.facturacion.infrastructure.repositories.InvoiceBatchRepository;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class InvoiceService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final InvoiceBatchRepositoryPort repository;
  private final BsaleApiAdapter bsaleApiAdapter;
  private final InvoiceHistoryRepositoryPort invoiceHisRp;
  private final EmailService emailService;
  private final Executor taskExecutor;
  private final String notifyTo;

  public InvoiceService(
      InvoiceBatchRepository repo,
      BsaleApiAdapter adapter,
      InvoiceHistoryRepositoryPort rp,
      EmailService emailService,
      @Qualifier("taskExecutor") Executor taskExecutor,
      @Value("${NOTIFY_REPORT_EMAIL}") String notifyTo) {
    this.repository = repo;
    this.bsaleApiAdapter = adapter;
    this.invoiceHisRp = rp;
    this.emailService = emailService;
    this.taskExecutor = taskExecutor;
    this.notifyTo = notifyTo;
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
  public BaseAPIResponse<Boolean> pregenerateHeaders(@NonNull Workload rGenerate) {

    Boolean response = this.repository.pregenerateInvoices(rGenerate, "system-user");

    return BaseAPIResponse.success("Datos pregenerados correctamente", response);
  }

  /**
   * 
   * @param workload
   * @return
   */
  public BaseAPIResponse<String> generateInvoices(@NonNull String workload) {

    List<InvoiceHeader> invoices = this.invoiceHisRp.findPendingInvoicesByWorkload(workload);

    if (invoices.isEmpty()) {
      this.logger.warn("Not pending invoices for: {}", workload);
      return BaseAPIResponse.error("No se encontraron facturas pendientes para el código de carga: " + workload);
    }

    this.logger.info("Pending invoices: {}", invoices.size());

    List<@NonNull InvoiceHeader> successfulInvoices = new ArrayList<>();

    for (var invoice : invoices) {
      try {

        this.logger.info("Invoice for-> OS/OC: {} and NI: {}", invoice.getObservation(), invoice.getIncomingNumber());
        var response = bsaleApiAdapter.createExternalInvoice(invoice);

        if (response != null && response.getId() != null) {

          invoice.setSerialNumber(response.getSerialNumber());
          invoice.setDocumentId(response.getId());
          invoice.setDocumentUrl(response.getUrlPdfOriginal());

          invoiceHisRp.updateInvoiceStatus(
              invoice.getHistoryId(),
              response.getSerialNumber(),
              response.getId());

          successfulInvoices.add(invoice);

        }

      } catch (Exception e) {
        this.logger.error("Error processing Invoice ID " + invoice.getHistoryId() +
            ": " + e.getMessage());
      }
    }

    String message = String.format("Proceso completado. %d de %d facturas emitidas exitosamente.",
        successfulInvoices.size(), invoices.size());

    this.logger.info("Invoices generated: {}", successfulInvoices.size());

    CompletableFuture.runAsync(() -> {
      this.logger.info("Generating report async mode...");
      this.sendFullReport(workload, successfulInvoices, message);
    }, taskExecutor);

    return BaseAPIResponse.success(
        "Facturas generadas existosamente, se enviará un email de confirmación en unos minutos",
        message);
  }

  @SuppressWarnings("null")
  private void sendFullReport(@NonNull String workload, List<@NonNull InvoiceHeader> successfulInvoices,
      String reportMessage) {

    try {
      // Zip downloaded files
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));
      var downloadedInvoices = this.downloadGeneratedInvoices(successfulInvoices);
      byte[] zipBytes = ZipSysHelper.compressFiles(downloadedInvoices);

      var zipFileName = new StringBuilder()
          .append(timestamp)
          .append("-")
          .append("facturas.zip")
          .toString();

      var invoicesZip = FileModelDTO.builder()
          .filename(zipFileName)
          .fileBytes(zipBytes)
          .build();

      // Create excel report
      var reportExcelFile = this.generateTableReport(workload);

      // Send email report
      var attachments = List.of(invoicesZip, reportExcelFile);

      this.emailService.sendEmailWithAttachments(
          this.notifyTo,
          "Reporte de facturacion",
          reportMessage,
          false,
          attachments);
    } catch (Exception e) {
      this.logger.error("Error sendig report", e);
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

      var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
      var timestamp = LocalDateTime.now().format(formatter);
      var filename = String.format("%s-reporte-facturacion.xlsx", timestamp);

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

  @SuppressWarnings("null")
  public List<FileModelDTO> downloadGeneratedInvoices(List<InvoiceHeader> invoices) {

    List<CompletableFuture<FileModelDTO>> futures = invoices.stream()
        .map(invoice -> CompletableFuture.supplyAsync(() -> {
          try {
            byte[] pdfBytes = this.bsaleApiAdapter.downloadBsaleDocument(invoice.getDocumentUrl());
            if (pdfBytes != null && pdfBytes.length > 0) {
              String safeFileName = invoice.getSerialNumber() + ".pdf";
              return FileModelDTO.builder()
                  .filename(safeFileName)
                  .fileBytes(pdfBytes)
                  .fileType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                  .build();
            }
          } catch (Exception e) {
            this.logger.error("Error downloading " + invoice.getSerialNumber(), e);
          }
          return null;
        }, taskExecutor))
        .toList();

    return futures.stream()
        .map(CompletableFuture::join)
        .filter(java.util.Objects::nonNull)
        .toList();
  }
}
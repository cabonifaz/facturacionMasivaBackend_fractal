package org.app.facturacion.services;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.app.facturacion.application.mappers.ActivityReportRow;
import org.app.facturacion.application.mappers.ExcelReader;
import org.app.facturacion.domain.exceptions.SystemAPIException;
import org.app.facturacion.domain.exceptions.ValidationAPIException;
import org.app.facturacion.domain.models.FileModelDTO;
import org.app.facturacion.domain.models.ReportActivityDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ReportService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final PDFService pdfService;
  private final Executor taskExecutor;

  public byte[] generateAllPdfsAsync(MultipartFile file) {

    this.logger.info("Processing file in service: {}", file.getOriginalFilename());

    ExcelReader reader = new ExcelReader();
    var rows = reader.readActivityReportDocument(file);

    this.logger.info("Lines read: {}", rows.size());

    record GroupingKey(Integer incommingNote, String ocOs, String invoiceSerial, String collaborator) {
    }

    @SuppressWarnings("null")
    Map<GroupingKey, List<ActivityReportRow>> groupedData = rows.stream()
        .collect(Collectors.groupingBy(row -> new GroupingKey(
            row.getIncommingNote(),
            row.getOcOs(),
            row.getInvoiceSerial(),
            row.getResourceName())));

    this.logger.info("Groups {} found", groupedData.size());

    groupedData.forEach((key, rowsInGroup) -> {
      long uniquePeople = rowsInGroup.stream()
          .map(ActivityReportRow::getResourceName)
          .distinct()
          .count();

      if (uniquePeople > 1) {
        logger.error("Alert! {} group contains {} differente collaborators: {}",
            key.invoiceSerial(),
            uniquePeople,
            rowsInGroup.stream().map(ActivityReportRow::getResourceName).collect(Collectors.toSet()));
        throw new ValidationAPIException("Se indentifico un reporte de actividades con 2 colaboradores diferentes");
      }
    });

    this.logger.info("Creating reports files...");

    // List of promises,this can create multiples reports at the same time
    List<CompletableFuture<FileModelDTO>> futures = groupedData.entrySet().stream()
        .map(entry -> CompletableFuture.supplyAsync(() -> {

          GroupingKey key = entry.getKey();
          ReportActivityDTO pdfDto = mapToDto(entry.getValue());

          this.logger.info("Generating PDF for: {}", pdfDto.getCollaborator());
          byte[] pdfBytes = pdfService.generatePdf(pdfDto);

          // Create file name
          String fileName = String.format("CELER - %s - %s.pdf", key.ocOs(), pdfDto.getCollaborator())
              .replaceAll("[\\\\/:*?\"<>|]", "_");

          return FileModelDTO.builder()
              .filename(fileName)
              .fileBytes(pdfBytes)
              .build();
        }, taskExecutor)) // This taskExecutor can create 5-10 reports, configuration at AsyncConfig
        .toList();

    // Wait for list of promises
    List<FileModelDTO> completedPdfs = futures.stream()
        .map(CompletableFuture::join)
        .toList();

    this.logger.info("All {} PDFs generated. Starting ZIP compression...", completedPdfs.size());

    // Compress files
    try (var baos = new ByteArrayOutputStream();
        var zos = new ZipOutputStream(baos)) {

      for (FileModelDTO pdfFile : completedPdfs) {
        ZipEntry zipEntry = new ZipEntry(pdfFile.getFilename());
        zos.putNextEntry(zipEntry);
        zos.write(pdfFile.getFileBytes());
        zos.closeEntry();
      }

      zos.finish();

      this.logger.info("Files compressed");

      return baos.toByteArray();

    } catch (Exception e) {
      logger.error("Error generating report zip", e);
      throw new SystemAPIException("Error generating reports", e);
    }
  }

  private ReportActivityDTO mapToDto(List<ActivityReportRow> rows) {
    if (rows.isEmpty())
      return new ReportActivityDTO();

    ActivityReportRow first = rows.get(0);
    ReportActivityDTO dto = new ReportActivityDTO();

    // Header Data
    dto.setCompany(first.getProvider() != null ? first.getProvider() : "CELER SAC");
    dto.setCollaborator(first.getResourceName());
    dto.setProfile(first.getResourceProfile());

    // Get current Date
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM-dd", new Locale("es", "ES"));
    String raw = LocalDate.now().format(formatter);
    String emissionDate = Character.toUpperCase(raw.charAt(0)) + raw.substring(1);

    dto.setEmissionDate(emissionDate);

    // Details
    List<ReportActivityDTO.ReportDetails> detailsList = new ArrayList<>();
    for (ActivityReportRow row : rows) {
      ReportActivityDTO.ReportDetails detail = new ReportActivityDTO.ReportDetails();

      detail.setTicket(row.getPucharseOrder());
      detail.setOs(row.getOcOs());
      detail.setActivityPeriod(row.getServicePeriod());

      String cleanId = row.getInitiativeId() != null
          ? row.getInitiativeId().replaceAll("\\.0+$", "")
          : null;

      detail.setInitiativeNumber(cleanId);
      detail.setActivities(row.getActivities());

      detail.setDeliverable(row.getActivitiesDetails());
      detail.setIncomingNote(String.valueOf(row.getIncommingNote()));
      detail.setInvoice(row.getInvoiceSerial());
      detail.setFeedback(row.getFeedback());
      detail.setManagement(row.getManagment());
      detail.setManager(row.getManager());

      detailsList.add(detail);
    }
    dto.setDetails(detailsList);

    return dto;
  }

}

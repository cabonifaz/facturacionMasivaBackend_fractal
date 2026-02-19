package org.app.facturacion.services;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.app.facturacion.domain.exceptions.SystemAPIException;
import org.app.facturacion.domain.exceptions.ValidationAPIException;
import org.app.facturacion.domain.models.ReportActivityDTO;
import org.app.facturacion.infrastructure.mappers.ActivityReportRow;
import org.app.facturacion.infrastructure.mappers.ExcelReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReportService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final PDFService pdfService;

  public ReportService(PDFService pdfService) {
    this.pdfService = pdfService;
  }

  @SuppressWarnings("null")
  public byte[] generateActivityReport(MultipartFile file) {

    this.logger.info("Processing file in service: {}", file.getOriginalFilename());

    ExcelReader reader = new ExcelReader();
    var rows = reader.readActivityReportDocument(file);

    this.logger.info("Lines read: {}", rows.size());

    record GroupingKey(Integer incommingNote, String ocOs, String invoiceSerial, String collaborator) {
    }

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

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos)) {

      for (Map.Entry<GroupingKey, List<ActivityReportRow>> entry : groupedData.entrySet()) {
        GroupingKey key = entry.getKey();
        List<ActivityReportRow> groupRows = entry.getValue();

        ReportActivityDTO pdfDto = mapToDto(groupRows);

        // Generate PDF
        byte[] pdfBytes = pdfService.generatePdf(pdfDto);

        // Add to ZIP
        var fileName = new StringBuilder()
            .append("CELER")
            .append(" - ")
            .append(key.ocOs)
            .append(" - ")
            .append(pdfDto.getCollaborator())
            .append(".pdf")
            .toString();

        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        ZipEntry zipEntry = new ZipEntry(fileName);
        zos.putNextEntry(zipEntry);
        zos.write(pdfBytes);
        zos.closeEntry();
      }

      zos.finish();
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

  public byte[] createSendEmailReport(MultipartFile zipFile) {

    this.logger.info("File received: {}", zipFile.getOriginalFilename());
    this.logger.info("File size: {} bytes", zipFile.getSize());

    return null;
  }

}

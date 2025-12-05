package org.app.facturacion.infrastructure.controller;

import org.app.facturacion.application.services.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/reports")
public class ReportController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final ReportService reportService;

  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @PostMapping("/create-activity-report")
  public ResponseEntity<byte[]> createActivityReport(
      @RequestParam("file") MultipartFile file) {
    this.logger.info("Creating report from: {}", file.getOriginalFilename());
    byte[] zipFile = this.reportService.generateActivityReport(file);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reportes.zip\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(zipFile);
  }

  @PostMapping("/create-send-report-email")
  public ResponseEntity<byte[]> createAndSendEMailReport(
      @RequestParam("file") MultipartFile file) {

    this.reportService.createSendEmailReport(file);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reportes.zip\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(null);

  }

}

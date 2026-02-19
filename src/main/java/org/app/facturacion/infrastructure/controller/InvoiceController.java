package org.app.facturacion.infrastructure.controller;

import org.app.facturacion.application.services.InvoiceService;
import org.app.facturacion.domain.models.BaseAPIResponse;
import org.app.facturacion.domain.models.Workload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.NonNull;

@RestController()
@RequestMapping("/invoices-batch")
@Validated
public class InvoiceController {

  private Logger logger = LoggerFactory.getLogger(InvoiceController.class);

  private final InvoiceService service;

  public InvoiceController(InvoiceService service) {
    this.service = service;
  }

  @PostMapping("/upload-batch")
  public BaseAPIResponse<String> processSheetFile(
      @RequestParam("file") MultipartFile file) {

    logger.info("Processing file...");
    logger.info("Original filename: {}", file.getOriginalFilename());
    logger.info("Size: {}", file.getSize());
    logger.info("ContentType: {}", file.getContentType());

    BaseAPIResponse<String> response = this.service.processInvoceBatchFile(file);
    return BaseAPIResponse.success(response.getMessage(), response.getData());
  }

  @PostMapping("/create-details")
  public BaseAPIResponse<Boolean> createDetailsForWorkLoad(
      @RequestBody() Workload workload) {

    BaseAPIResponse<Boolean> response = this.service.createDetailsForWorkLoad(workload.getWorkloadId());
    return BaseAPIResponse.success(response.getMessage(), response.getData());
  }

  @PostMapping("/pre-generate")
  public BaseAPIResponse<Boolean> pregenerateInvoices(
      @RequestBody() @NonNull Workload workload) {

    this.logger.debug("Data from client: {}", workload.getWorkloadId());

    BaseAPIResponse<Boolean> rs = this.service.pregenerateHeaders(workload);

    return BaseAPIResponse.success(rs.getMessage(), rs.getData());
  }

  @PostMapping("/create-invoices")
  public ResponseEntity<BaseAPIResponse<String>> generateInvoices(
      @RequestBody @NonNull Workload request) {

    this.logger.info("Generating Invoices for: {}", request.getWorkloadId());

    var rs = this.service.generateInvoices(request.getWorkloadId());

    return ResponseEntity.ok(rs);
  }

  @PostMapping("/table-report")
  public ResponseEntity<byte[]> generateTableReport(
      @RequestBody @NonNull Workload request) {

    this.logger.info("Generating Report for: {}", request.getWorkloadId());

    var rs = this.service.generateTableReport(request.getWorkloadId());
    var excelMediaType = MediaType
        .parseMediaType(rs.getFileType());

    return ResponseEntity.ok()
        .contentType(excelMediaType)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + rs.getFilename() + "\"")
        .body(rs.getFileBytes());
  }

}

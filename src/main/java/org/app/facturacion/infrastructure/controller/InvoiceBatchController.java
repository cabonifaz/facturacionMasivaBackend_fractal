package org.app.facturacion.infrastructure.controller;

import org.app.facturacion.application.services.InvoiceBatchService;
import org.app.facturacion.domain.models.BaseAPIResponse;
import org.app.facturacion.domain.models.InvoicePreGenerate;
import org.app.facturacion.domain.models.Workload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class InvoiceBatchController {

  private Logger logger = LoggerFactory.getLogger(InvoiceBatchController.class);

  private final InvoiceBatchService service;

  public InvoiceBatchController(InvoiceBatchService service) {
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
      @RequestBody() @NonNull InvoicePreGenerate request) {

    this.logger.debug("Data from client: {}", request);

    BaseAPIResponse<Boolean> rs = this.service.pregenerateHeaders(request);

    return BaseAPIResponse.success(rs.getMessage(), rs.getData());
  }

  @PostMapping("/create-invoices")

  public BaseAPIResponse<String> generateInvoices(
      @RequestBody @NonNull Workload request) {

    this.logger.info("Generating Invoices for: {}", request.getWorkloadId());

    BaseAPIResponse<String> rs = this.service.generateInvoices(request.getWorkloadId());

    return BaseAPIResponse.success("Datos procesados correctamente", rs.getData());
  }

}

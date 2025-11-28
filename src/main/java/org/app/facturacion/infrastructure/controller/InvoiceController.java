package org.app.facturacion.infrastructure.controller;

import org.app.facturacion.application.services.InvoiceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("invoices")
public class InvoiceController {

  private final InvoiceService invoiceService;

  public InvoiceController(InvoiceService invoiceService) {
    this.invoiceService = invoiceService;
  }

  @GetMapping("/download/{serialNumber}/{documentId}")
  public ResponseEntity<byte[]> getInvoiceFile(
      @PathVariable("documentId") Long documentId,
      @PathVariable("serialNumber") String serialNumber) {

    byte[] pdfBytes = this.invoiceService.downloadInvoiceFile(documentId);

    String filename = serialNumber + "_" + documentId + ".pdf";

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(pdfBytes);
  }

}

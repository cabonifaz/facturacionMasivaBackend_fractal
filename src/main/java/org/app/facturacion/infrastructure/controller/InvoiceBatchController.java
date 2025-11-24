package org.app.facturacion.infrastructure.controller;

import org.app.facturacion.domain.models.BaseAPIResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController()
@RequestMapping("/invoices-batch")
public class InvoiceBatchController {

  public InvoiceBatchController() {

  }

  @PostMapping("/upload-batch")
  public BaseAPIResponse<Integer> processSheetFile(@RequestParam("file") MultipartFile file) {
    return new BaseAPIResponse<>(
        "File received: " + file.getOriginalFilename(),
        null);
  }
}

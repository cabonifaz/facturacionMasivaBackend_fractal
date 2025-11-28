package org.app.facturacion.application.services;

import org.app.facturacion.infrastructure.api.adapter.BsaleApiAdapter;
import org.springframework.stereotype.Service;

@Service
public class InvoiceService {

  private final BsaleApiAdapter bsaleApiAdapter;

  public InvoiceService(BsaleApiAdapter bsaleApiAdapter) {
    this.bsaleApiAdapter = bsaleApiAdapter;
  }

  public byte[] downloadInvoiceFile(Long documentId) {
    String url = this.bsaleApiAdapter.getDocumentInvoiceDetails(documentId).getUrlPdfOriginal();
    return this.bsaleApiAdapter.downloadBsaleDocument(url);
  }
}

package org.app.facturacion.domain.port;

import java.util.List;

import org.app.facturacion.domain.models.InvoiceRow;
import org.eclipse.jdt.annotation.NonNull;

public interface InvoiceBatchRepositoryPort {

  String addOrUpdateInvoiceWorkspace(
      List<InvoiceRow> invoices,
      @NonNull String username);
}

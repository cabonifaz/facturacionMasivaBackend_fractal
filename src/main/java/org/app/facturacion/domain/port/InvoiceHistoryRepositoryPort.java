package org.app.facturacion.domain.port;

import java.util.List;

import org.app.facturacion.domain.models.InvoiceHeader;
import org.eclipse.jdt.annotation.NonNull;

public interface InvoiceHistoryRepositoryPort {

  /**
   * Busca todas las cabeceras de facturas consolidadas listas para ser enviadas a
   * la API externa.
   * 
   * @param workload El ID del bloque de trabajo.
   * @return Lista de
   */
  List<InvoiceHeader> findPendingInvoicesByWorkload(@NonNull String workload);

  /**
   * Actualiza el historial con los datos de la factura generada
   * 
   * @param historyId
   * @param invoiceNumber
   * @param documentId
   */
  void updateInvoiceStatus(
      Integer historyId,
      String invoiceNumber,
      Long documentId);

}

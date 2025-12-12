package org.app.facturacion.domain.port;

import java.util.List;

import org.app.facturacion.domain.models.InvoicePreGenerate;
import org.app.facturacion.domain.models.InvoiceRow;
import org.app.facturacion.domain.models.InvoicesTableReport;
import org.eclipse.jdt.annotation.NonNull;

public interface InvoiceBatchRepositoryPort {

  /**
   * Procesa la carga de trabajo
   * 
   * @return retorna el código de carga de los datos procesados
   */
  String addOrUpdateInvoiceWorkspace(
      List<InvoiceRow> invoices,
      @NonNull String username);

  /**
   * Genera el detalle de facturación para una carga de trabajo
   * 
   * @param workloadId ID de la carga de trabajo
   * @return Retorna true si se generan los detalles para la carga de trabajo
   */
  Boolean createDetailsForWorkload(@NonNull String workload, @NonNull String username);

  /**
   * Pre-genera la factura con sus respectivas cabeceras y otros detalles
   * constantes
   * 
   * @return Retorna true si el procedimiento termina con éxito
   */
  Boolean pregenerateInvoices(@NonNull InvoicePreGenerate reqGenerate, @NonNull String username);

  List<InvoicesTableReport> getTableReportByWorkload(@NonNull String workload);

}

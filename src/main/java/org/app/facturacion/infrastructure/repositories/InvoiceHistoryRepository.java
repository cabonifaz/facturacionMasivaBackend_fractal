package org.app.facturacion.infrastructure.repositories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.app.facturacion.domain.exceptions.SystemAPIException;
import org.app.facturacion.domain.models.InvoiceHistory;
import org.app.facturacion.domain.models.InvoiceHistoryDetails;
import org.app.facturacion.domain.port.InvoiceHistoryRepositoryPort;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceHistoryRepository implements InvoiceHistoryRepositoryPort {

  private Logger logger = LoggerFactory.getLogger(getClass());
  private final JdbcTemplate jdbcTemplate;

  public InvoiceHistoryRepository(JdbcTemplate jTemplate) {
    this.jdbcTemplate = jTemplate;
  }

  @Override
  public List<InvoiceHistory> findPendingInvoicesByWorkload(@NonNull String workload) {

    this.logger.info("Getting pending invoices for workload: {}", workload);

    final String SP_NAME = "SPP_FACTURA_CABECERA_PENDIENTES_LIST";
    SimpleJdbcCall jSimpleJdbcCall = new SimpleJdbcCall(jdbcTemplate);
    jSimpleJdbcCall.withProcedureName(SP_NAME);

    this.logger.debug("Calling to SP: {}", SP_NAME);

    Map<String, Object> result = jSimpleJdbcCall.execute(workload);

    if (result == null || result.isEmpty())
      throw new SystemAPIException("No hubo respuesta de la base de datos", null);

    // 1. Obtener Result Set #1 (Mensaje)
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> responseList = (List<Map<String, Object>>) result.getOrDefault("#result-set-1",
        Collections.emptyList());

    // 2. Obtener Result Set #2 (Cabeceras)
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> invoicesList = (List<Map<String, Object>>) result.getOrDefault("#result-set-2",
        Collections.emptyList());

    // 3. Obtener Result Set #3 (Detalles) - ¡NUEVO!
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> detailsListRaw = (List<Map<String, Object>>) result.getOrDefault("#result-set-3",
        Collections.emptyList());

    if (invoicesList.isEmpty())
      throw new SystemAPIException("No se encontraron cabeceras de facturas", null);

    // Validación del mensaje de estado
    Map<String, Object> responseMap = responseList.get(0);
    Integer idMessage = (Integer) responseMap.getOrDefault("ID_TIPO_MENSAJE", 3);
    String message = (String) responseMap.getOrDefault("MENSAJE", "Sin mensaje de error de la DB");

    if (idMessage != 2)
      throw new SystemAPIException(message, null);

    List<InvoiceHistory> invoices = new ArrayList<>();

    // 4. Iterar Cabeceras
    for (Map<String, Object> invoiceRow : invoicesList) {

      InvoiceHistory history = new InvoiceHistory();

      // Mapeo de cabecera
      history.setWorkload((String) invoiceRow.get("CODIGO_CARGA"));
      history.setObservation((String) invoiceRow.get("OBSERVACION"));
      history.setClientAddress((String) invoiceRow.get("DIRECCION_CLIENTE"));
      history.setClientDistrict((String) invoiceRow.get("DISTRITO_CLIENTE"));
      history.setClientCity((String) invoiceRow.get("CIUDAD_CLIENTE"));
      history.setClientCode((String) invoiceRow.get("CODIGO_CLIENTE"));
      history.setClientActivity((String) invoiceRow.get("ACTIVIDAD_CLIENTE"));
      history.setHistoryId((Integer) invoiceRow.get("ID"));
      history.setState((Integer) invoiceRow.get("ID_ESTADO_REGISTRO"));

      Integer currentOrderNumber = (Integer) invoiceRow.get("NUM_ORDEN");
      history.setOrderNumber(currentOrderNumber);

      // 5. Mapear Detalles asociados a esta cabecera
      List<InvoiceHistoryDetails> details = new ArrayList<>();

      for (Map<String, Object> detailRow : detailsListRaw) {
        Integer detailOrderNum = (Integer) detailRow.get("NUM_ORDEN");

        if (currentOrderNumber.equals(detailOrderNum)) {
          InvoiceHistoryDetails detail = new InvoiceHistoryDetails();

          // Mapeo básico
          detail.setOrderNumber(detailOrderNum);
          detail.setConcept((String) detailRow.get("CONCEPTO") + " " + history.getObservation());
          detail.setIncomingNumber((Integer) detailRow.get("NUM_NOTA_INGRESO"));
          detail.setQuantity((Integer) detailRow.get("CANTIDAD"));

          // Mapeo seguro de Decimales (SQL Decimal -> Java Double)
          // Usamos 'Number' para evitar ClassCastException entre BigDecimal y Double
          if (detailRow.get("SUB_TOTAL") != null)
            detail.setSubTotal(((Number) detailRow.get("SUB_TOTAL")).doubleValue());

          if (detailRow.get("MONTO_UNITARIO") != null)
            detail.setAmountPerUnit(((Number) detailRow.get("MONTO_UNITARIO")).doubleValue());

          // Asumimos que DESCUENTO puede venir nulo o como número
          if (detailRow.containsKey("DESCUENTO") && detailRow.get("DESCUENTO") != null) {
            detail.setDiscount(((Number) detailRow.get("DESCUENTO")).doubleValue());
          } else {
            detail.setDiscount(0.0);
          }

          details.add(detail);
        }
      }

      // Asignar la lista de detalles al padre
      history.setDetails(details);

      invoices.add(history);
    }

    return invoices;
  }

  @Override
  public void updateInvoiceStatus(Integer headerId, String invoiceSerial, Long documentId) {
    this.logger.info("Updating invoice status: {}", headerId);

    final String SP_NAME = "SPP_FACTURA_CABECERA_COMPLETE_UPD";
    SimpleJdbcCall jSimpleJdbcCall = new SimpleJdbcCall(jdbcTemplate);
    jSimpleJdbcCall.withProcedureName(SP_NAME);

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("ID_CABECERA", headerId)
        .addValue("NUM_FACTURA", invoiceSerial)
        .addValue("ID_DOCUMENTO", documentId);

    this.logger.debug("Calling to SP: {}", SP_NAME);

    Map<String, Object> result = jSimpleJdbcCall.execute(params);

    if (result == null || result.isEmpty())
      throw new SystemAPIException("No hubo respuesta de la base de datos", null);

    // 1. Obtener Result Set #1 (Mensaje)
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> responseList = (List<Map<String, Object>>) result.getOrDefault("#result-set-1",
        Collections.emptyList());

    // Validación del mensaje de estado
    Map<String, Object> responseMap = responseList.get(0);
    Integer idMessage = (Integer) responseMap.getOrDefault("ID_TIPO_MENSAJE", 3);
    String message = (String) responseMap.getOrDefault("MENSAJE", "Sin mensaje de error de la DB");

    if (idMessage != 2)
      throw new SystemAPIException(message, null);
  }

}

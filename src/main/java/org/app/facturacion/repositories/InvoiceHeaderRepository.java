package org.app.facturacion.repositories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.app.facturacion.domain.exceptions.SystemAPIException;
import org.app.facturacion.domain.models.InvoiceHeader;
import org.app.facturacion.domain.port.InvoiceHistoryRepositoryPort;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Repository
public class InvoiceHeaderRepository implements InvoiceHistoryRepositoryPort {

  private Logger logger = LoggerFactory.getLogger(getClass());
  private final JdbcTemplate jdbcTemplate;

  public InvoiceHeaderRepository(JdbcTemplate jTemplate) {
    this.jdbcTemplate = jTemplate;
  }

  @Override
  public List<InvoiceHeader> findPendingInvoicesByWorkload(@NonNull String workload) {
    this.logger.info("Getting pending invoices (JSON mode) for: {}", workload);

    SimpleJdbcCall jSimpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
        .withProcedureName("SPP_FACTURA_CABECERA_PENDIENTES_JSON");

    Map<String, Object> result = jSimpleJdbcCall.execute(workload);

    // 1. Validaciones básicas de mensaje (Result Set 1)
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> responseList = (List<Map<String, Object>>) result.getOrDefault("#result-set-1",
        Collections.emptyList());
    if (responseList.isEmpty()) {
      this.logger.error("Response: {}", result);
      throw new SystemAPIException("No hubo respuesta de la base de datos", null);
    }

    Map<String, Object> baseResponseDb = responseList.get(0);
    Integer messageId = (Integer) baseResponseDb.getOrDefault("ID_TIPO_MENSAJE", 3);
    String message = (String) baseResponseDb.getOrDefault("MENSAJE", "Sin respuesta personalizada de la DB");

    if (messageId != 2)
      throw new SystemAPIException(message, null);

    // 2. Obtener el JSON (Result Set 2)
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> jsonRows = (List<Map<String, Object>>) result.getOrDefault("#result-set-2",
        Collections.emptyList());

    if (jsonRows.isEmpty()) {
      return new ArrayList<>();
    }

    /*
     * Concatenar el JSON (En caso de que sea muy largo, SQL Server lo parte en
     * varias filas)
     */
    StringBuilder jsonBuilder = new StringBuilder();
    for (Map<String, Object> row : jsonRows) {
      jsonBuilder.append(row.values().iterator().next());
    }

    String jsonString = jsonBuilder.toString();

    try {

      // 3. Convertir JSON String a List<InvoiceHeader>
      var objectMapper = new ObjectMapper();
      return objectMapper.readValue(jsonString, new TypeReference<List<InvoiceHeader>>() {
      });

    } catch (Exception e) {
      this.logger.error("Error deserializing Invoice JSON from DB", e);
      throw new SystemAPIException("Error al procesar la data de facturación", e);
    }
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

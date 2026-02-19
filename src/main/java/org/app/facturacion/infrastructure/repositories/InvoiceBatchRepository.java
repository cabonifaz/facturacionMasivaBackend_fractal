package org.app.facturacion.infrastructure.repositories;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.app.facturacion.domain.exceptions.SystemAPIException;
import org.app.facturacion.domain.models.InvoicePreGenerate;
import org.app.facturacion.domain.models.InvoiceRow;
import org.app.facturacion.domain.models.InvoicesTableReport;
import org.app.facturacion.domain.port.InvoiceBatchRepositoryPort;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import com.microsoft.sqlserver.jdbc.SQLServerDataTable;
import com.microsoft.sqlserver.jdbc.SQLServerException;

@SuppressWarnings("unchecked")
@Repository
public class InvoiceBatchRepository implements InvoiceBatchRepositoryPort {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  private final JdbcTemplate jdbcTemplate;

  public InvoiceBatchRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String addOrUpdateInvoiceWorkspace(List<InvoiceRow> facturas,
      @NonNull String username) {

    final String SP_NAME = "SPP_FACTURACION_TRABAJO_INS";
    this.logger.debug("Calling to SP: {}", SP_NAME);

    SimpleJdbcCall jSimpleJdbcCall = new SimpleJdbcCall(jdbcTemplate);
    jSimpleJdbcCall.withProcedureName(SP_NAME);

    try {
      SQLServerDataTable tvpData = createTvpFromList(facturas);

      Map<String, Object> result = jSimpleJdbcCall.execute(
          1,
          username,
          1,
          tvpData);

      this.logger.debug("Data from DB: {}", result);

      if (result == null || result.isEmpty())
        throw new SystemAPIException("No hubo respuesta de la base de datos", null);

      List<Map<String, Object>> responseList = (List<Map<String, Object>>) result.getOrDefault("#result-set-1",
          Collections.emptyList());

      if (responseList.isEmpty())
        throw new SystemAPIException("No hubo respuesta de la base de datos", null);

      Map<String, Object> responseMap = responseList.get(0);

      Integer idMessage = (Integer) responseMap.getOrDefault("ID_TIPO_MENSAJE", 3);
      String message = (String) responseMap.getOrDefault("MENSAJE", "Sin mensaje de error de la DB");

      if (idMessage != 2)
        throw new SystemAPIException(message, null);

      List<Map<String, Object>> workLoadList = (List<Map<String, Object>>) result.getOrDefault("#result-set-2",
          Collections.emptyList());

      Map<String, Object> workLoadMap = workLoadList.get(0);

      String workLoadId = (String) workLoadMap.get("CODIGO_CARGA");

      if (workLoadId == null)
        throw new SystemAPIException("La base de datos no devolvió el ID de carga", null);

      return workLoadId;

    } catch (SQLServerException e) {
      this.logger.error("Error: {}", e);
      throw new SystemAPIException("Ha ocurrido un error guardando en la base de datos", e);
    }

  }

  private SQLServerDataTable createTvpFromList(List<InvoiceRow> invoices) throws SQLServerException {

    SQLServerDataTable tvp = new SQLServerDataTable();

    tvp.addColumnMetadata("ID", Types.INTEGER);
    tvp.addColumnMetadata("CODIGO_CARGA", Types.VARCHAR);
    tvp.addColumnMetadata("NOMBRE_CLIENTE", Types.VARCHAR);
    tvp.addColumnMetadata("OC_OS", Types.VARCHAR);
    tvp.addColumnMetadata("NI_CS", Types.INTEGER);
    tvp.addColumnMetadata("COLABORADOR", Types.VARCHAR);
    tvp.addColumnMetadata("FCH_INICIO", Types.VARCHAR);
    tvp.addColumnMetadata("FCH_FIN", Types.VARCHAR);
    tvp.addColumnMetadata("CONCEPTO", Types.VARCHAR);
    tvp.addColumnMetadata("MONEDA", Types.VARCHAR);
    tvp.addColumnMetadata("MONTO", Types.DECIMAL);
    tvp.addColumnMetadata("IGV", Types.DECIMAL);
    tvp.addColumnMetadata("TOTAL", Types.DECIMAL);
    tvp.addColumnMetadata("CONTACTO", Types.VARCHAR);
    tvp.addColumnMetadata("N_FACTURA", Types.VARCHAR);
    tvp.addColumnMetadata("EDITADO", Types.BIT);
    tvp.addColumnMetadata("ID_ESTADO_REGISTRO", Types.INTEGER);

    for (InvoiceRow row : invoices) {
      tvp.addRow(
          row.getId(),
          row.getWorkloadId(),
          row.getClientName(),
          row.getOcOs(),
          row.getNiCs(),
          row.getCollaborator(),
          row.getStartDate(),
          row.getEndDate(),
          row.getConcept(),
          row.getCurrency(),
          row.getAmount(),
          row.getIgv(),
          row.getTotalAmount(),
          row.getContact(),
          row.getInvoiceNumber(),
          row.isEdited());
    }

    return tvp;
  }

  @Override
  public Boolean createDetailsForWorkload(@NonNull String workload, @NonNull String username) {

    this.logger.info("Creating details for workload: {}", workload);

    final String SP_NAME = "SPP_FACTURA_DETALLE_INS_BATCH";
    SimpleJdbcCall jSimpleJdbcCall = new SimpleJdbcCall(jdbcTemplate);
    jSimpleJdbcCall.withProcedureName(SP_NAME);

    this.logger.debug("Calling to SP: {}", SP_NAME);

    Map<String, Object> result = jSimpleJdbcCall.execute(
        1,
        username,
        1,
        workload);

    this.logger.debug("Data from DB: {}", result);

    if (result == null || result.isEmpty())
      throw new SystemAPIException("No hubo respuesta de la base de datos", null);

    List<Map<String, Object>> responseList = (List<Map<String, Object>>) result.getOrDefault("#result-set-1",
        Collections.emptyList());

    if (responseList.isEmpty())
      throw new SystemAPIException("No hubo respuesta de la base de datos", null);

    Map<String, Object> responseMap = responseList.get(0);

    Integer idMessage = (Integer) responseMap.getOrDefault("ID_TIPO_MENSAJE", 3);
    String message = (String) responseMap.getOrDefault("MENSAJE", "Sin mensaje de error de la DB");

    if (idMessage != 2)
      throw new SystemAPIException(message, null);

    return true;

  }

  @Override
  public Boolean pregenerateInvoices(@NonNull InvoicePreGenerate reqGenerate, @NonNull String username) {

    this.logger.info("Pre-generate for workload: {}", reqGenerate.getWorkload());

    final String SP_NAME = "SPP_FACTURA_CABECERA_GEN";
    SimpleJdbcCall jSimpleJdbcCall = new SimpleJdbcCall(jdbcTemplate);
    jSimpleJdbcCall.withProcedureName(SP_NAME);

    this.logger.debug("Calling to SP: {}", SP_NAME);

    Map<String, Object> result = jSimpleJdbcCall.execute(
        username,
        reqGenerate.getWorkload());

    this.logger.debug("Data from DB: {}", result);

    if (result == null || result.isEmpty())
      throw new SystemAPIException("No hubo respuesta de la base de datos", null);

    List<Map<String, Object>> responseList = (List<Map<String, Object>>) result.getOrDefault("#result-set-1",
        Collections.emptyList());

    if (responseList.isEmpty())
      throw new SystemAPIException("No hubo respuesta de la base de datos", null);

    Map<String, Object> responseMap = responseList.get(0);

    Integer idMessage = (Integer) responseMap.getOrDefault("ID_TIPO_MENSAJE", 3);
    String message = (String) responseMap.getOrDefault("MENSAJE", "Sin mensaje de error de la DB");

    if (idMessage != 2)
      throw new SystemAPIException(message, null);

    return true;
  }

  @Override
  public List<InvoicesTableReport> getTableReportByWorkload(@NonNull String workload) {
    this.logger.info("Getting report for workload: {}", workload);
    final String SP_NAME = "SPP_REPORTE_FACTURAS";

    var jSimpleJdbcCall = new SimpleJdbcCall(jdbcTemplate);
    jSimpleJdbcCall.withProcedureName(SP_NAME);
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("CODIGO_CARGA", workload);
    Map<String, Object> result = jSimpleJdbcCall.execute(params);
    if (result == null || result.isEmpty())
      throw new SystemAPIException("No hubo respuesta de la base de datos", null);

    var reportResultSet = (List<Map<String, Object>>) result.getOrDefault("#result-set-1",
        Collections.emptyList());

    if (reportResultSet.isEmpty()) {
      this.logger.warn("Empty report received from DB");
      return new ArrayList<>();
    }

    this.logger.info("Report received from DB");

    return reportResultSet.stream().map(row -> {
      var reportBuilder = InvoicesTableReport.builder();
      reportBuilder.incomingNumber((Integer) row.get("NOTA_INGRESO"));
      reportBuilder.startDate((String) row.get("FCH_INICIO"));
      reportBuilder.endDate((String) row.get("FCH_FIN"));
      reportBuilder.concept((String) row.get("CONCEPTO"));
      reportBuilder.pricePerUnit((BigDecimal) row.get("MONTO_UNITARIO"));
      reportBuilder.clientName((String) row.get("CLIENTE"));
      reportBuilder.analytic((String) row.get("ANALITICA"));
      reportBuilder.igv((BigDecimal) row.get("IGV"));
      reportBuilder.totalToPay((BigDecimal) row.get("TOTAL_PAGAR"));
      reportBuilder.contact((String) row.get("CONTACTO"));
      reportBuilder.invoiceNumber((String) row.get("NUM_FACTURA"));
      reportBuilder.currencyName((String) row.get("MONEDA"));
      reportBuilder.collaborator((String) row.get("COLABORADOR"));
      reportBuilder.observation((String) row.get("OBSERVACION"));
      return reportBuilder.build();
    }).collect(Collectors.toList());
  }

}

package org.app.facturacion.adapters;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.app.facturacion.adapters.bsale.config.InvoiceConfig;
import org.app.facturacion.adapters.bsale.dto.BsaleDocumentDetailDTO;
import org.app.facturacion.adapters.bsale.dto.BsaleInvoiceResponseDTO;
import org.app.facturacion.domain.exceptions.SystemAPIException;
import org.app.facturacion.domain.models.InvoiceHeader;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class BsaleApiAdapter {

  private final RestTemplate restTemplate;
  private final String bsaleApiUrl;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final String bsaleToken;
  private final InvoiceConfig invoiceConfig;

  private final String API_VERSION = "/v1";

  public BsaleApiAdapter(
      RestTemplate restTemplate,
      @Value("${bsale.api.url}") String externalApiUrl,
      @Value("${bsale.api.token}") String bsaleToken, InvoiceConfig invoiceConfig) {

    this.restTemplate = restTemplate;
    this.bsaleApiUrl = externalApiUrl;
    this.bsaleToken = bsaleToken;
    this.invoiceConfig = invoiceConfig;
  }

  /**
   * Descarga el PDF binario de una factura específica desde Bsale.
   * 
   * @param documentId El ID numérico del documento en Bsale (ej: 55070).
   * @return El archivo PDF como un array de bytes (byte[]).
   */
  public BsaleDocumentDetailDTO getDocumentInvoiceDetails(Long documentId) {

    String fullUrl = bsaleApiUrl + API_VERSION + "/documents/" + documentId + ".json";
    this.logger.info("Getting details from: {}", fullUrl);

    // Basic Auth
    HttpHeaders headers = new HttpHeaders();
    headers.set("access_token", this.bsaleToken);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    HttpEntity<String> entity = new HttpEntity<>(null, headers);

    try {
      ResponseEntity<@NonNull BsaleDocumentDetailDTO> response = restTemplate.exchange(
          fullUrl,
          HttpMethod.GET,
          entity,
          BsaleDocumentDetailDTO.class);

      if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
        this.logger.info("Process completed: {}", documentId);
        return response.getBody();
      }

      this.logger.error("Error getting details for document ID {}. Status: {}", documentId, response.getStatusCode());

      throw new SystemAPIException(
          "No se pudo obtener detalles del documento de Bsale. Código HTTP: " + response.getStatusCode(),
          null);

    } catch (RestClientException e) {
      this.logger.error("Communication error during getting document details", e);
      throw new SystemAPIException("Error de comunicación al obtener detalles del documento.", e);
    }
  }

  public byte[] downloadBsaleDocument(@NonNull String url) {

    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", "Mozilla/5.0 ");

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    this.logger.info("Downloading document from: {}", url);

    int maxRetries = 5;
    int retryDelayMs = 2000;

    for (int attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        this.logger.debug("Attemp {} of {} to download from: {}", attempt, maxRetries, url);

        ResponseEntity<byte[]> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            byte[].class);

        // Validar éxito HTTP y que el cuerpo no sea nulo
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
          byte[] fileContent = response.getBody();

          if (fileContent.length > 0) {
            return fileContent;
          } else {
            this.logger.warn("Attemp {}: Successffully download (200 OK) but file has 0 bytes. Trying again...",
                attempt);
          }
        } else if (response.getStatusCode().is3xxRedirection()) {
          this.logger.info("Redirección detectada en intento {}", attempt);
        }

      } catch (RestClientException e) {
        this.logger.warn("Error de comunicación en intento {}: {}", attempt, e.getMessage());
      }

      if (attempt < maxRetries) {
        try {
          Thread.sleep(retryDelayMs);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new SystemAPIException("El hilo fue interrumpido durante la espera de reintento", ie);
        }
      }
    }

    this.logger.error("Fallo definitivo al descargar PDF tras {} intentos desde {}", maxRetries, url);
    throw new SystemAPIException(
        "Error al descargar el PDF: Archivo vacío o error de conexión tras múltiples intentos.", null);
  }

  public BsaleInvoiceResponseDTO createExternalInvoice(@NonNull Map<String, Object> jsonBody) {

    HttpHeaders headers = new HttpHeaders();
    headers.set("access_token", this.bsaleToken);
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(jsonBody, headers);

    StringBuilder sBuilder = new StringBuilder();
    sBuilder.append(bsaleApiUrl);
    sBuilder.append("/v1/documents.json");

    try {

      ResponseEntity<@NonNull BsaleInvoiceResponseDTO> response = restTemplate.exchange(
          sBuilder.toString(),
          HttpMethod.POST,
          entity,
          BsaleInvoiceResponseDTO.class);

      if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
        this.logger.info("Document ID created by Bsale: {}", response.getBody());
        return response.getBody();
      }

      this.logger.error("Error creating invoice. Status: {}", response.getStatusCode());
      throw new SystemAPIException("Bsale API ha fallado codigo HTTP: " + response.getStatusCode(), null);

    } catch (HttpStatusCodeException e) {
      this.logger.error("Bsale API error. Status: {} Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new SystemAPIException(
          "Bsale HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString(), e);
    } catch (RestClientException e) {
      this.logger.error("Communication error with Bsale API", e);
      throw new SystemAPIException("Error al comunicarse con Bsale", e);
    } catch (Exception e) {
      this.logger.error("Unknow error calling to Bsale API", e);
      throw new SystemAPIException("Ha ocurrido un error desconocido llamando a la API de Bsale", e);
    }
  }

  public Map<String, Object> mapToBsaleStructure(InvoiceHeader source) {

    LocalDate todayPeru = LocalDate.now(ZoneId.of("America/Lima"));

    Long emissionTimestamp = todayPeru.atStartOfDay(ZoneId.of("America/Lima"))
        .toEpochSecond();
    Long expirationTimestamp = todayPeru.plusDays(22)
        .atStartOfDay(ZoneId.of("America/Lima"))
        .toEpochSecond();

    // 2. Procesar Detalles y Calcular Total Base
    List<Map<String, Object>> detailsList = new ArrayList<>();
    BigDecimal totalNeto = BigDecimal.ZERO;

    for (var d : source.getDetails()) {
      // Lógica: (Precio * Cantidad) - Descuento
      double discount = d.getDiscount() != null ? d.getDiscount() : 0.0;
      BigDecimal rowNet = BigDecimal.valueOf(d.getAmountPerUnit())
          .multiply(BigDecimal.valueOf(d.getQuantity()))
          .subtract(BigDecimal.valueOf(discount));

      totalNeto = totalNeto.add(rowNet);

      // Crear el Map del detalle para Bsale
      Map<String, Object> detailMap = new HashMap<>();
      detailMap.put("netUnitValue", d.getAmountPerUnit());
      detailMap.put("quantity", d.getQuantity());
      detailMap.put("comment", d.getConcept());
      detailMap.put("discount", discount);
      detailMap.put("taxes", Collections.singletonList(
          Map.of("code", this.invoiceConfig.getTaxId(), "percentage", 18)));

      detailsList.add(detailMap);
    }

    // 3. CÁLCULO DE MONTOS (Lo que necesitas)
    BigDecimal totalIgv = totalNeto.multiply(new BigDecimal("0.18"));
    BigDecimal totalFacturado = totalNeto.add(totalIgv).setScale(2, RoundingMode.HALF_UP);

    List<Map<String, Object>> paymentsList = new ArrayList<>();

    // --- APLICA DETRACCIÓN ---
    BigDecimal detractionTax = new BigDecimal("0.12");

    // Calculamos monto detracción
    BigDecimal detractionAmount = totalFacturado.multiply(detractionTax).setScale(2, RoundingMode.HALF_UP);

    // Calculamos PRIMERA CUOTA (Total - Detracción)
    BigDecimal firstDue = totalFacturado.subtract(detractionAmount);

    // Pago 1: Lo que paga el cliente (Primera Cuota)
    Map<String, Object> pagoPrincipal = new HashMap<>();
    pagoPrincipal.put("paymentTypeId", this.invoiceConfig.getPaymentTypes().getDue());
    pagoPrincipal.put("amount", firstDue.doubleValue());
    pagoPrincipal.put("recordDate", expirationTimestamp);
    paymentsList.add(pagoPrincipal);

    // Pago 2: La Detracción
    paymentsList.add(createDetractionPayment(detractionAmount.doubleValue(), expirationTimestamp));

    // 4. Armar el objeto raíz
    Map<String, Object> root = new HashMap<>();
    root.put("documentTypeId", this.invoiceConfig.getDocumentTypeId());
    root.put("officeId", this.invoiceConfig.getOfficeId());
    root.put("coinId", this.invoiceConfig.getCoinId());
    root.put("emissionDate", emissionTimestamp);
    root.put("expirationDate", expirationTimestamp);

    // Mapeo Cliente simple
    Map<String, Object> client = new HashMap<>();
    client.put("code", source.getClientCode());
    client.put("address", source.getClientAddress());
    client.put("district", source.getClientDistrict());
    client.put("city", source.getClientCity());
    client.put("province", source.getClientProvince());

    root.put("client", client);
    root.put("details", detailsList);
    root.put("payments", paymentsList);

    List<Map<String, Object>> dynAttributesList = new ArrayList<>();

    // 1. Atributo OC
    dynAttributesList.add(Map.of(
        "description", source.getObservation(),
        "dynamicAttributeId", this.invoiceConfig.getDynamicAttributeOcId()));

    // 2. Atributo de tipo factura
    dynAttributesList.add(Map.of(
        "description", this.invoiceConfig.getDetraction().getOperationType().getValueId(),
        "dynamicAttributeId", this.invoiceConfig.getDetraction().getOperationType().getFormId()));

    // Asignamos la lista completa
    root.put("dynamicAttributes", dynAttributesList);

    return root;
  }

  private Map<String, Object> createDetractionPayment(Double amount, Long recordDate) {
    Map<String, Object> payment = new HashMap<>();
    payment.put("paymentTypeId", this.invoiceConfig.getPaymentTypes().getDetraction());
    payment.put("amount", amount);
    payment.put("recordDate", recordDate);

    List<Map<String, Object>> contactDetails = new ArrayList<>();

    // Medio de pago - Detracción
    contactDetails.add(buildContactDetail(
        this.invoiceConfig.getDetraction().getPaymentMethod().getFormId(),
        this.invoiceConfig.getDetraction().getPaymentMethod().getValueId()));

    // Cuenta Bancaria - Detracción
    contactDetails.add(buildContactDetail(
        this.invoiceConfig.getDetraction().getBankAccount().getFormId(),
        this.invoiceConfig.getDetraction().getBankAccount().getValueId()));

    // Bien / Servicio - Detracción
    contactDetails.add(buildContactDetail(
        this.invoiceConfig.getDetraction().getServiceCode().getFormId(),
        this.invoiceConfig.getDetraction().getServiceCode().getValueId()));

    payment.put("contactDetails", contactDetails);
    return payment;
  }

  private Map<String, Object> buildContactDetail(Integer dynamicId, Integer detailAttrId) {
    Map<String, Object> detail = new HashMap<>();
    detail.put("dynamicAttributeId", dynamicId);
    detail.put("description", detailAttrId);
    detail.put("detailAtributeContact", Collections.singletonList(Map.of("detailAtributeId", detailAttrId)));
    return detail;
  }
}
package org.app.facturacion.infrastructure.api.adapter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.app.facturacion.domain.exceptions.SystemAPIException;
import org.app.facturacion.domain.models.InvoiceHistoryDetails;
import org.app.facturacion.infrastructure.api.adapter.bsale.dto.BsaleDocumentDetailDTO;
import org.app.facturacion.infrastructure.api.dto.BsaleApiInvoiceRequestDTO;
import org.app.facturacion.infrastructure.api.dto.BsaleInvoiceResponseDTO;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.Builder;
import lombok.Data;

@Service
public class BsaleApiAdapter {

  private final RestTemplate restTemplate;
  private final String bsaleApiUrl;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final String bsaleToken;

  private final String API_VERSION = "/v1";

  public BsaleApiAdapter(
      RestTemplate restTemplate,
      @Value("${bsale.api.url}") String externalApiUrl,
      @Value("${bsale.api.token}") String bsaleToken) {

    this.restTemplate = restTemplate;
    this.bsaleApiUrl = externalApiUrl;
    this.bsaleToken = bsaleToken;
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
            this.logger.warn("Intento {}: Descarga exitosa (200 OK) pero el archivo tiene 0 bytes. Reintentando...",
                attempt);
          }
        } else if (response.getStatusCode().is3xxRedirection()) {
          this.logger.info("Redirección detectada en intento {}", attempt);
        }

      } catch (RestClientException e) {
        this.logger.warn("Error de comunicación en intento {}: {}", attempt, e.getMessage());
      }

      // Si no fue el último intento, esperamos antes de reintentar
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

  public BsaleInvoiceResponseDTO createExternalInvoice(@NonNull BsaleApiInvoiceRequestDTO request) {

    HttpHeaders headers = new HttpHeaders();
    headers.set("access_token", this.bsaleToken);
    headers.setContentType(MediaType.APPLICATION_JSON);

    // 1. MAPEO MANUAL: Convertimos tu DTO plano al objeto estructurado que pide
    // Bsale
    BsaleJsonStructure jsonBody = mapToBsaleStructure(request);

    // 2. Enviamos el objeto estructurado (jsonBody) en lugar del request original
    HttpEntity<BsaleJsonStructure> entity = new HttpEntity<>(jsonBody, headers);

    try {
      String fullUrl = bsaleApiUrl + API_VERSION + "/documents.json";

      @SuppressWarnings("null")
      ResponseEntity<BsaleInvoiceResponseDTO> response = restTemplate.exchange(
          fullUrl,
          HttpMethod.POST,
          entity,
          BsaleInvoiceResponseDTO.class);

      if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
        this.logger.info("Document ID created by Bsale: {}", response.getBody());
        return response.getBody();
      }

      this.logger.error("Error creating invoice. Status: {}", response.getStatusCode());
      throw new SystemAPIException("Bsale API ha fallado codigo HTTP: " + response.getStatusCode(), null);

    } catch (RestClientException e) {
      this.logger.error("Communication error with Bsale API", e);
      throw new SystemAPIException("Error al comunicarse con Bsale", e);
    } catch (Exception e) {
      this.logger.error("Communication error or JSON mapping failure with Bsale API", e);
      throw new SystemAPIException("Error al comunicarse con Bsale o mapear respuesta.", e);
    }
  }

  private BsaleJsonStructure mapToBsaleStructure(BsaleApiInvoiceRequestDTO source) {
    long currentTimestamp = Instant.now().getEpochSecond();

    // 1. Mapeo del Cliente
    BsaleClient client = BsaleClient.builder()
        .code(source.getCode())
        .address(source.getAddress())
        .district(source.getDistrict())
        .city(source.getCity())
        .company(source.getCompany())
        .activity(source.getActivity())
        .build();

    // 2. Mapeo de Detalles
    List<BsaleDetail> details = new ArrayList<>();
    @SuppressWarnings("unused")
    double totalAmount = 0.0;

    for (InvoiceHistoryDetails d : source.getDetails()) {

      double rowTotal = (d.getAmountPerUnit() * d.getQuantity()) - (d.getDiscount() != null ? d.getDiscount() : 0);
      totalAmount += rowTotal;

      details.add(BsaleDetail.builder()
          .netUnitValue(d.getAmountPerUnit())
          .quantity(d.getQuantity())
          .taxId("[3]") // [1]=IGV (18%), [3]=Exento
          .comment(d.getConcept())
          .discount(d.getDiscount() != null ? d.getDiscount() : 0)
          .build());
    }

    // 3. Mapeo de Pagos (Calculado con el total de los detalles)
    /*
     * BsalePayment payment = BsalePayment.builder()
     * .paymentTypeId(source.getPaymentId()) // ID del medio de pago
     * .amount((int) Math.round(totalAmount))
     * .recordDate(currentTimestamp)
     * .build();
     */

    // 4. Construcción del Objeto Raíz
    return BsaleJsonStructure.builder()
        .documentTypeId(source.getDocumentTypeId())
        .officeId(1) // Hardcodeado según tu ejemplo (o sácalo de properties)
        .emissionDate(currentTimestamp)
        .observation(source.getObservation())
        .client(client)
        .details(details)
        // .payments(Collections.singletonList(payment))
        .build();
  }

  @Data
  @Builder
  private static class BsaleJsonStructure {
    private Integer documentTypeId;
    private Integer officeId;
    private Long emissionDate;
    private String observation;
    private BsaleClient client;
    private List<BsaleDetail> details;
    private List<BsalePayment> payments;
  }

  @Data
  @Builder
  private static class BsaleClient {
    private String code;
    private String address;
    private String district;
    private String city;
    private String company;
    private String activity;
  }

  @Data
  @Builder
  private static class BsaleDetail {
    private Double netUnitValue;
    private Integer quantity;
    private String taxId; // Ej: "[1]" o "[3]"
    private String comment;
    private Double discount;
  }

  @Data
  @Builder
  private static class BsalePayment {
    private Integer paymentTypeId;
    private Integer amount;
    private Long recordDate;
  }
}
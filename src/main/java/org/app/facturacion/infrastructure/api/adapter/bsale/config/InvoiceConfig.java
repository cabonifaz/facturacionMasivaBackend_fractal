package org.app.facturacion.infrastructure.api.adapter.bsale.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "bsale.config.invoice")
public class InvoiceConfig {

  private Integer officeId;
  private Integer documentTypeId;
  private Integer coinId;
  private Long taxId;
  private PaymentTypes paymentTypes = new PaymentTypes();
  private DetractionAttributes detraction = new DetractionAttributes();
  private Integer dynamicAttributeOcId;

  @Data
  public static class PaymentTypes {
    private Integer due;
    private Integer detraction;
  }

  @Data
  public static class DetractionAttributes {

    private AttributePair paymentMethod;
    private AttributePair bankAccount;
    private AttributePair serviceCode;
    private AttributePair operationType;
  }

  @Data
  public static class AttributePair {
    private Integer formId;
    private Integer valueId;
  }
}
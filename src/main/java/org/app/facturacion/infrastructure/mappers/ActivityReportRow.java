package org.app.facturacion.infrastructure.mappers;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActivityReportRow {

  private String provider;
  private String pucharseOrder;
  private String ocOs;
  private String initiativeId;
  private String resourceName;
  private String resourceProfile;
  private String servicePeriod;
  private String activities;
  private String activitiesDetails;
  private String manager;
  private String managment;
  private String feedback;
  private Integer incommingNote;
  private String invoiceSerial;
}

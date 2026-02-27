package org.app.facturacion.domain.models;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import lombok.Data;

@Data
public class ReportActivityDTO {

  @Value("report.config.company: CELER SAC")
  private String company;
  private String collaborator;

  @Value("report.config.emission-data: Dic-25")
  private String emissionDate;
  private String profile;
  private List<ReportDetails> details;

  @Data
  public static class ReportDetails {
    private String ticket;
    private String os;
    private String activityPeriod;
    private String initiativeNumber;
    private String activities;
    private String deliverable;
    private String incomingNote;
    private String invoice;
    private String feedback;
    private String manager;
    private String management;
  }

}

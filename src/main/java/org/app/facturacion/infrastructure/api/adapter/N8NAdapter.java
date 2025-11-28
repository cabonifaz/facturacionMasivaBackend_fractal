package org.app.facturacion.infrastructure.api.adapter;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class N8NAdapter {

  private final RestTemplate restTemplate;
  private final String n8nUrl;
  private final String createExecelHook;

  public N8NAdapter(
      RestTemplate restTemplate,
      @Value("${n8n.base.url}") String n8nUrl,
      @Value("${n8n.hooks.create-excel}") String createExcelHook) {

    this.restTemplate = restTemplate;
    this.n8nUrl = n8nUrl;
    this.createExecelHook = createExcelHook;
  }

  public void callCreateExcelHook(String workload) {

    String url = n8nUrl + "/" + this.createExecelHook;

    Map<String, String> body = Map.of("workload", workload);
    this.restTemplate.postForLocation(url, body);
  }

}

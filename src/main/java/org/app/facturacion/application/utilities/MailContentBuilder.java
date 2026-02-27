package org.app.facturacion.application.utilities;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class MailContentBuilder {
  private final SpringTemplateEngine templateEngine;

  public String buildInvoiceReportHtml(String title, String dynamicActionMessage) {
    Context context = new Context();
    context.setVariable("title", title);
    context.setVariable("message", dynamicActionMessage);
    return templateEngine.process("report-message", context);
  }
}

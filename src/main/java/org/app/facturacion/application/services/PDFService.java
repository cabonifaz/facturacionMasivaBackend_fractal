package org.app.facturacion.application.services;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import org.app.facturacion.domain.exceptions.SystemAPIException;
import org.app.facturacion.domain.models.ReportActivityDTO;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;

@Service
public class PDFService {

  private final SpringTemplateEngine templateEngine;

  public PDFService(SpringTemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  public byte[] generatePdf(ReportActivityDTO data) {
    Context context = new Context();
    context.setVariable("report", data);

    String htmlContent = templateEngine.process("individual-report", context);

    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

      PdfRendererBuilder builder = new PdfRendererBuilder();

      String baseUrl = getClass().getResource("/static/images/").toExternalForm();
      builder.withUri(baseUrl);

      builder.useFastMode();
      builder.withHtmlContent(htmlContent, baseUrl);
      builder.toStream(os);
      builder.run();

      return os.toByteArray();
    } catch (Exception e) {
      throw new SystemAPIException("Error creating PDF", e);
    }
  }
}

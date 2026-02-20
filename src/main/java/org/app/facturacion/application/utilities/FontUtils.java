package org.app.facturacion.application.utilities;

import java.io.InputStream;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.springframework.stereotype.Component;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

@Component
public class FontUtils {

  public record FontDefinition(
      String resourcePath,
      String fontFamily,
      int weight,
      PdfRendererBuilder.FontStyle style) {
  }

  private final List<@NonNull FontDefinition> FONTS = List.of(
      new FontDefinition("/fonts/arial.ttf", "Arial", 400, PdfRendererBuilder.FontStyle.NORMAL),
      new FontDefinition("/fonts/arialbd.ttf", "Arial", 700, PdfRendererBuilder.FontStyle.NORMAL),
      new FontDefinition("/fonts/ariali.ttf", "Arial", 400, PdfRendererBuilder.FontStyle.ITALIC),
      new FontDefinition("/fonts/arialbi.ttf", "Arial", 700, PdfRendererBuilder.FontStyle.ITALIC),
      new FontDefinition("/fonts/zapfdingbats.ttf", "ZapfDingbats", 400, PdfRendererBuilder.FontStyle.NORMAL));

  public void registerFonts(PdfRendererBuilder builder) {
    for (FontDefinition font : FONTS) {
      builder.useFont(
          () -> getFontStream(font.resourcePath()),
          font.fontFamily(),
          font.weight(),
          font.style(),
          true);
    }
  }

  private InputStream getFontStream(String resourcePath) {
    InputStream stream = getClass().getResourceAsStream(resourcePath);
    if (stream == null) {
      throw new IllegalStateException("Font not found at classpath: " + resourcePath);
    }
    return stream;
  }
}
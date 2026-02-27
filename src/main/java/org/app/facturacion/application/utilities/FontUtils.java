package org.app.facturacion.application.utilities;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNull;
import org.springframework.stereotype.Component;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import jakarta.annotation.PostConstruct;

@Component
public class FontUtils {

  public record FontDefinition(
      String resourcePath,
      String fontFamily,
      int weight,
      PdfRendererBuilder.FontStyle style) {
  }

  // Cache fonts in memory, to avoid loading everytime
  private final Map<String, byte[]> fontCache = new ConcurrentHashMap<>();

  private final List<@NonNull FontDefinition> FONTS = List.of(
      new FontDefinition("/fonts/arial.ttf", "Arial", 400, PdfRendererBuilder.FontStyle.NORMAL),
      new FontDefinition("/fonts/arialbd.ttf", "Arial", 700, PdfRendererBuilder.FontStyle.NORMAL),
      new FontDefinition("/fonts/ariali.ttf", "Arial", 400, PdfRendererBuilder.FontStyle.ITALIC),
      new FontDefinition("/fonts/arialbi.ttf", "Arial", 700, PdfRendererBuilder.FontStyle.ITALIC),
      new FontDefinition("/fonts/zapfdingbats.ttf", "ZapfDingbats", 400, PdfRendererBuilder.FontStyle.NORMAL));

  @PostConstruct
  public void init() {
    for (FontDefinition font : FONTS) {
      try (InputStream is = getClass().getResourceAsStream(font.resourcePath())) {
        if (is == null)
          throw new RuntimeException("Font not found: " + font.resourcePath());
        fontCache.put(font.resourcePath(), is.readAllBytes());
      } catch (IOException e) {
        throw new RuntimeException("Error loadign font in memory", e);
      }
    }
  }

  public void registerFonts(PdfRendererBuilder builder) {
    for (FontDefinition font : FONTS) {
      byte[] data = fontCache.get(font.resourcePath());
      // Usamos los bytes de la memoria, ¡ya no hay lectura de disco!
      builder.useFont(() -> new ByteArrayInputStream(data),
          font.fontFamily(), font.weight(), font.style(), true);
    }
  }
}
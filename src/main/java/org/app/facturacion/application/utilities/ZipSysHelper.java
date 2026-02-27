package org.app.facturacion.application.utilities;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.app.facturacion.domain.models.FileModelDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ZipSysHelper {

  private final Logger logger = LoggerFactory.getLogger(ZipSysHelper.class);

  /**
   * Comprime una lista de archivos (FileModelDTO) en un solo archivo ZIP.
   *
   * @param files Lista de archivos a comprimir.
   * @return byte[] El archivo ZIP en arreglo de bytes. Si algo falla retorna un
   *         arreglo vacío
   */
  public static byte[] compressFiles(List<FileModelDTO> files) {

    if (files == null || files.isEmpty())
      return new byte[0];

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos)) {

      for (FileModelDTO file : files) {
        if (file.getFileBytes() == null || file.getFileBytes().length == 0) {
          logger.warn("Skipping empty file: {}", file.getFilename());
          continue;
        }

        // Create safe filename
        String safeFileName = sanitizeFilename(file.getFilename());

        // Create zip entry
        ZipEntry entry = new ZipEntry(safeFileName);
        zos.putNextEntry(entry);

        // Write bytes
        zos.write(file.getFileBytes());
        zos.closeEntry();
      }

      zos.finish();
      return baos.toByteArray();
    } catch (Exception e) {
      logger.error("Error compressing files", e);
      return new byte[0];
    }

  }

  /**
   * Limpia el nombre del archivo de caracteres ilegales para el sistema de
   * archivos.
   */
  public static String sanitizeFilename(String filename) {
    if (filename == null)
      return "unknown_file";
    return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
  }
}

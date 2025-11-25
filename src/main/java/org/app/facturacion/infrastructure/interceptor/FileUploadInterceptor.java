package org.app.facturacion.infrastructure.interceptor;

import java.util.List;

import org.app.facturacion.domain.exceptions.ValidationException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FileUploadInterceptor implements HandlerInterceptor {

  private static final List<String> ALLOWED_TYPES = List.of(
      "application/vnd.ms-excel",
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "application/pdf");

  @Override
  public boolean preHandle(HttpServletRequest request,
      HttpServletResponse response,
      Object handler) {
    try {

      if (request instanceof MultipartHttpServletRequest multipartRequest) {

        MultipartFile file = multipartRequest.getFile("file");

        if (file == null || file.isEmpty()) {
          throw new ValidationException("No se subió ningun archivo");
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
          String fileContentType = file.getContentType();
          throw new ValidationException("Formato de archivo no permitido: " + fileContentType);
        }

      }
    } catch (MultipartException e) {
      throw new ValidationException("Error en el formato de la petición con archivo");
    }
    return true;
  }

}

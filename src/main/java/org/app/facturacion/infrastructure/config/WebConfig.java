package org.app.facturacion.infrastructure.config;

import org.app.facturacion.application.interceptors.FileUploadInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Autowired
  private FileUploadInterceptor fileUploadInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(fileUploadInterceptor)
        .addPathPatterns("/invoices/upload-batch");
  }

}

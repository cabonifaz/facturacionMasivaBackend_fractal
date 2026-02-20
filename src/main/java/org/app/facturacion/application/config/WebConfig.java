package org.app.facturacion.application.config;

import org.app.facturacion.application.interceptors.ApiKeyInterceptor;
import org.app.facturacion.application.interceptors.FileUploadInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Autowired
  private FileUploadInterceptor fileUploadInterceptor;

  @Autowired
  private ApiKeyInterceptor apiKeyInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {

    registry.addInterceptor(apiKeyInterceptor)
        .addPathPatterns("/**");

    registry.addInterceptor(fileUploadInterceptor)
        .addPathPatterns("/invoices/upload-batch");
  }

}

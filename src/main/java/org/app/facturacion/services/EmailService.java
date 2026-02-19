package org.app.facturacion.services;

import java.util.List;

import org.app.facturacion.domain.models.FileModelDTO;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;

@Service
public class EmailService {

  private final JavaMailSender mailSender;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public EmailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  /**
   * Envía en email al distanario con múltiples archivos adjuntos
   * 
   * @param to
   * @param subject
   * @param body
   * @param isHtmlBody
   * @param attachments Lista de archivos adjuntos
   */

  @Async("taskExecutor")
  public void sendEmailWithAttachments(
      String to,
      String subject,
      String body,
      boolean isHtmlBody,
      @NonNull List<FileModelDTO> attachments) {
    try {

      this.logger.info("Sending Email with {} attachments", attachments.size());
      this.logger.info("Sending email to: {}", to);
      this.logger.info("Subject: {}", subject);

      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setTo(to);
      helper.setSubject(subject);

      // True for plain text, false for html
      helper.setText(body, isHtmlBody);

      for (var attachment : attachments) {
        var dataSource = new ByteArrayDataSource(
            attachment.getFileBytes(),
            MediaType.APPLICATION_OCTET_STREAM_VALUE);
        helper.addAttachment(attachment.getFilename(), dataSource);
      }

      mailSender.send(message);

      this.logger.info("Email sent to: {} Subjet: {}", to, subject);

    } catch (MessagingException e) {
      this.logger.error("Email not sent: {}", e);
    }
  }

  /**
   * Envía un email a un destinatario específico, soporta HTML y texto plano
   * 
   * @param to         Destinatario del correo
   * @param subject    Asunto del correo
   * @param body       Cuerpo del correo, HTML o Texto plano
   * @param isHtmlBody true para indicar que es HTML
   */
  @Async("taskExecutor")
  public void sendEmail(
      String to,
      String subject,
      String body,
      boolean isHtmlBody) {
    MimeMessage message = mailSender.createMimeMessage();

    this.logger.debug("Mailsender: {}", this.mailSender.toString());

    try {
      this.logger.info("Sending Email without attachments...");
      this.logger.info("Sending email to: {}", to);
      this.logger.info("Subject: {}", subject);

      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setTo(to);
      helper.setSubject(subject);

      // True for plain text, false for html
      helper.setText(body, isHtmlBody);

      mailSender.send(message);
      this.logger.info("Email sent to: {}", to);
    } catch (MessagingException e) {
      this.logger.error("Error sending email: {}", e);
    }
  }

}

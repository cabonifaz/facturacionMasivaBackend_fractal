package org.app.facturacion.application.services;

import org.app.facturacion.domain.exceptions.SystemAPIException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;

@Service
public class EmailService {

  private final JavaMailSender mailSender;

  public EmailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void sendEmailWithAttachment(
      String to,
      String subject,
      String body,
      byte[] attachment,
      String attachmentName) {
    MimeMessage message = mailSender.createMimeMessage();

    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setTo(to);
      helper.setSubject(subject);

      // True for plain text, false for html
      helper.setText(body, true);

      helper.addAttachment(attachmentName, new ByteArrayDataSource(attachment, "application/zip"));

      mailSender.send(message);

    } catch (MessagingException e) {
      throw new SystemAPIException("Falló al enviar el correo", e);
    }
  }
}

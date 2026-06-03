package com.njplastic.njplastic_api.auth.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import com.njplastic.njplastic_api.config.exceptions.BaseApiInternalServerErrorException;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  @Mock
  private JavaMailSender mailSender;

  private EmailService emailService;

  @BeforeEach
  void setUp() {
    EmailProperties properties = new EmailProperties("no-reply@njplastic.com", "https://app.njplastic.com/reset", 30);
    emailService = new EmailService(mailSender, properties);
  }

  private MimeMessage newMimeMessage() {
    return new MimeMessage(Session.getInstance(new Properties()));
  }

  @Test
  void sendPasswordReset_dispatchesEmail() {
    MimeMessage message = newMimeMessage();
    when(mailSender.createMimeMessage()).thenReturn(message);

    assertThatCode(() -> emailService.sendPasswordReset("user@njplastic.com", "opaque-token"))
        .doesNotThrowAnyException();

    verify(mailSender).send(message);
  }

  @Test
  void sendPasswordReset_wrapsMailFailureAsInternalServerError() {
    MimeMessage message = newMimeMessage();
    when(mailSender.createMimeMessage()).thenReturn(message);
    doThrow(new MailSendException("smtp down")).when(mailSender).send(message);

    assertThatThrownBy(() -> emailService.sendPasswordReset("user@njplastic.com", "opaque-token"))
        .isInstanceOf(BaseApiInternalServerErrorException.class);
  }

  @Test
  void sendReportDelivery_dispatchesAttachment() {
    MimeMessage message = newMimeMessage();
    when(mailSender.createMimeMessage()).thenReturn(message);

    assertThatCode(() -> emailService.sendReportDelivery(
        "manager@njplastic.com",
        "Report",
        "body",
        new byte[]{1, 2, 3},
        "report.csv",
        "text/csv")).doesNotThrowAnyException();

    verify(mailSender).send(message);
  }

  @Test
  void sendReportDelivery_wrapsMailFailureAsInternalServerError() {
    MimeMessage message = newMimeMessage();
    when(mailSender.createMimeMessage()).thenReturn(message);
    doThrow(new MailSendException("smtp down")).when(mailSender).send(message);

    assertThatThrownBy(() -> emailService.sendReportDelivery(
        "manager@njplastic.com", "Report", "body", new byte[]{1}, "r.csv", "text/csv"))
        .isInstanceOf(BaseApiInternalServerErrorException.class);
  }
}

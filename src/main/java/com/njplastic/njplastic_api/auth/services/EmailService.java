package com.njplastic.njplastic_api.auth.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.config.exceptions.BaseApiInternalServerErrorException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

/**
 * Outbound email gateway (EP-BE-02 reopened). Wraps Spring's
 * {@link JavaMailSender} so domain code never depends on Jakarta Mail directly.
 * Two product flows live here:
 * <ul>
 * <li>Password reset email - delivered after {@code POST /auth/password-reset};</li>
 * <li>Scheduled report delivery - attaches PDF/CSV/XLSX (EP-BE-08).</li>
 * </ul>
 *
 * <p>
 * Mail send failures are translated to {@code BaseApiInternalServerErrorException}
 * when the caller runs inside an HTTP request (per project convention - service
 * never re-throws raw {@link MailException}). The password-reset entry point
 * defensively wraps this in its own service to keep the flow idempotent for
 * the caller.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(EmailProperties.class)
public class EmailService {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
  private static final String UTF_8 = "UTF-8";

  private final JavaMailSender mailSender;
  private final EmailProperties properties;

  /**
   * Send a password reset email containing a clickable link with the given
   * opaque token. The link target is built from {@code app.mail.password-reset-base-url}.
   *
   * @param to    destination email address (already validated upstream)
   * @param token opaque token persisted on {@code password_reset_token}
   */
  public void sendPasswordReset(String to, String token) {
    String resetUrl = properties.passwordResetBaseUrl() + "?token=" + token;
    String body = """
        Hello,

        We received a request to reset the password for your NJPlastic account.
        Open the link below to choose a new password (it expires in %d minutes):

        %s

        If you did not request this reset, you can safely ignore this email.

        --
        NJPlastic
        """.formatted(properties.passwordResetTtlMinutes(), resetUrl);

    sendPlain(to, "NJPlastic - Password reset", body);
  }

  /**
   * Send a generated report as an email attachment.
   *
   * @param to       destination email address
   * @param subject  email subject line (e.g. "Shift report - 2026-06-01")
   * @param body     plain text body shown above the attachment
   * @param file     report bytes
   * @param filename suggested filename (e.g. "shift_2026-06-01.pdf")
   * @param mimeType IANA mime type (e.g. "application/pdf")
   */
  public void sendReportDelivery(String to, String subject, String body, byte[] file, String filename, String mimeType) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_8);
      helper.setFrom(properties.from());
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(body, false);
      helper.addAttachment(filename, new ByteArrayResource(file), mimeType);
      mailSender.send(message);
    } catch (MailException | MessagingException ex) {
      LOGGER.warn("Failed to send report email to [{}]: {}", to, ex.getMessage());
      throw new BaseApiInternalServerErrorException("Failed to send report email");
    }
  }

  private void sendPlain(String to, String subject, String body) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, UTF_8);
      helper.setFrom(properties.from());
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(body, false);
      mailSender.send(message);
    } catch (MailException | MessagingException ex) {
      LOGGER.warn("Failed to send email to [{}]: {}", to, ex.getMessage());
      throw new BaseApiInternalServerErrorException("Failed to send email");
    }
  }
}

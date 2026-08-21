package com.transgate.api.util;

import com.transgate.api.app.services.AppEnvironmentConfig;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SMTP mailer for mail.supersoft.com.ng (SSL / port 465).
 * Used for welcome and other transactional mail; separate from Habari/Gmail Mailers.
 */
@Component
public class SupersoftMailer {

    private static final Logger logger = Logger.getLogger(SupersoftMailer.class.getName());

    @Autowired
    private AppEnvironmentConfig appConfig;

    /**
     * @return true if SMTP accepted the message; false on failure (for RabbitMQ DLQ routing)
     */
    public boolean sendWelcomeMail(String userEmail, String userName, String userPassword,
            String firstname, String surname) {
        // TEMP testing log — remove or redact password before production
        logger.info(String.format(
                "[TEST] sendWelcomeMail invoked: userEmail=%s, userName=%s, userPassword=%s, firstname=%s, surname=%s",
                userEmail, userName, userPassword, firstname, surname));

        String displayName = ((firstname != null ? firstname : "") + " " + (surname != null ? surname : "")).trim();
        if (displayName.isEmpty()) {
            displayName = userName != null && !userName.isEmpty() ? userName : "User";
        }

        String safeEmail = escapeHtml(userEmail);
        String safeUserName = escapeHtml(userName);
        String safePassword = escapeHtml(userPassword);
        String safeName = escapeHtml(displayName);

        String html = "<!DOCTYPE html><html><body style=\"font-family: Arial, Helvetica, sans-serif; color: #222; line-height: 1.5;\">"
                + "<p>Dear " + safeName + ",</p>"
                + "<p>You have been invited to <strong>Belema's Portal</strong>. Here are your login details:</p>"
                + "<table style=\"border-collapse: collapse; margin: 16px 0;\">"
                + "<tr><td style=\"padding: 6px 12px 6px 0; font-weight: bold;\">Email</td><td style=\"padding: 6px 0;\">" + safeEmail + "</td></tr>"
                + "<tr><td style=\"padding: 6px 12px 6px 0; font-weight: bold;\">Username</td><td style=\"padding: 6px 0;\">" + safeUserName + "</td></tr>"
                + "<tr><td style=\"padding: 6px 12px 6px 0; font-weight: bold;\">Password</td><td style=\"padding: 6px 0;\">" + safePassword + "</td></tr>"
                + "</table>"
                + "<p>Please keep these credentials secure.</p>"
                + "<p>Cheers,<br/>Belema Team</p>"
                + "</body></html>";

        boolean sent = sendHtmlMail(userEmail, "Welcome to Belema Portal", html);
        logger.info("[TEST] sendWelcomeMail finished for " + userEmail + " sent=" + sent);
        return sent;
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public boolean sendHtmlMail(String toEmail, String subject, String htmlBody) {
        return sendHtmlMail(toEmail, subject, htmlBody, "");
    }

    public boolean sendHtmlMail(String toEmail, String subject, String htmlBody, String ccEmail) {
        String host = appConfig.getSupersoftMailHost();
        String port = appConfig.getSupersoftMailPort();
        String username = appConfig.getSupersoftMailUsername();
        String password = appConfig.getSupersoftMailPassword();
        String fromEmail = appConfig.getSupersoftMailFrom();

        if (fromEmail == null || fromEmail.isEmpty()) {
            fromEmail = username;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        // Force TLS 1.2 — old javax.mail 1.5 + modern JDKs otherwise fail with
        // "No appropriate protocol (protocol is disabled or cipher suites are inappropriate)"
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.checkserveridentity", "true");
        try {
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, new java.security.SecureRandom());
            javax.net.ssl.SSLSocketFactory factory = sslContext.getSocketFactory();
            props.put("mail.smtp.ssl.socketFactory", factory);
            props.put("mail.smtp.socketFactory", factory);
            props.put("mail.smtp.socketFactory.port", port);
            props.put("mail.smtp.socketFactory.fallback", "false");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to init TLSv1.2 SSLContext for Supersoft mail", e);
            props.put("mail.smtp.socketFactory.port", port);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        }

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setReplyTo(new InternetAddress[]{new InternetAddress(fromEmail)});
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            if (ccEmail != null && !ccEmail.isEmpty()) {
                message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccEmail));
            }
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");

            Transport.send(message);
            logger.info("Supersoft mail sent to " + toEmail);
            return true;
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "Supersoft mail error for " + toEmail + ": " + e.getMessage(), e);
            return false;
        }
    }
}

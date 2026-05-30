package ru.bakanov.eventreminder.notifications.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ru.bakanov.eventreminder.config.AppProperties;
import ru.bakanov.eventreminder.notifications.domain.NotificationPort;
import ru.bakanov.eventreminder.notifications.domain.models.NotificationChannel;
import ru.bakanov.eventreminder.notifications.domain.models.NotifyResult;

@Service
class EmailNotificationAdapter implements NotificationPort {

    private static final Logger LOG = LoggerFactory.getLogger(EmailNotificationAdapter.class);

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    EmailNotificationAdapter(JavaMailSender mailSender, AppProperties appProperties) {
        this.mailSender = mailSender;
        this.appProperties = appProperties;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public NotifyResult send(String recipient, String subject, String htmlBody) {
        if (recipient == null || recipient.isBlank()) {
            return NotifyResult.failed("Recipient email is empty");
        }
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(appProperties.mail().from());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            LOG.debug("Email sent to {}", recipient);
            return NotifyResult.ok();
        } catch (Exception e) {
            LOG.error("Failed to send email to {}: {}", recipient, e.getMessage());
            return NotifyResult.failed(e.getMessage());
        }
    }
}

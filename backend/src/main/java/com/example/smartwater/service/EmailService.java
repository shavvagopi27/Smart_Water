package com.example.smartwater.service;

import com.example.smartwater.entity.Alert;
import com.example.smartwater.entity.Invoice;
import com.example.smartwater.entity.User;
import com.example.smartwater.entity.WaterSavingTip;
import com.example.smartwater.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Sends notification e-mails. Credentials come from environment variables (see application.yml).
 * When app.mail.enabled=false (default) nothing is sent; the e-mail is only written to the log.
 * A mail failure never breaks the business operation that triggered it.
 */
@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${app.mail.from:no-reply@smartwater.local}")
    private String from;

    public void send(String to, String subject, String body, String attachmentName, byte[] attachment) {
        if (!enabled) {
            log.info("[MAIL DISABLED] to={} subject='{}' attachment={}", to, subject, attachmentName);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, attachment != null, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            if (attachment != null) {
                helper.addAttachment(attachmentName, new ByteArrayResource(attachment));
            }
            mailSender.send(message);
            log.info("E-mail sent to {} ({})", to, subject);
        } catch (Exception e) {
            log.error("Could not send e-mail to {}: {}", to, e.getMessage());
        }
    }

    public List<User> residentsOf(Long householdId) {
        return userRepository.findByHouseholdIdOrderByIdAsc(householdId).stream().filter(User::isEnabled).toList();
    }

    /** "Monthly bill generated" notification with the PDF attached (also covers invoice availability). */
    public void sendBillGenerated(Invoice inv, byte[] pdf) {
        String body = "Hello,\n\nYour water bill for " + inv.getBillingCycle().getName() + " (" + inv.getBillingCycle().getStartDate()
                + " to " + inv.getBillingCycle().getEndDate() + ") has been generated.\n\n"
                + "Invoice: " + inv.getInvoiceNumber() + "\nFlat: " + inv.getHousehold().getFlatNumber()
                + "\nConsumption: " + inv.getConsumption().toPlainString() + " kL"
                + "\nTotal amount due: \u20B9" + inv.getTotalAmount().toPlainString()
                + "\n\nThe invoice PDF is attached. You can also download it from the Smart Water portal.\n\nSmart Water Management";
        for (User u : residentsOf(inv.getHousehold().getId())) {
            send(u.getEmail(), "Your water bill " + inv.getInvoiceNumber() + " is ready", body, inv.getInvoiceNumber() + ".pdf", pdf);
        }
    }

    public void sendInvoiceAvailable(Invoice inv, byte[] pdf) {
        String body = "Hello,\n\nInvoice " + inv.getInvoiceNumber() + " for flat " + inv.getHousehold().getFlatNumber()
                + " is available. Amount due: \u20B9" + inv.getTotalAmount().toPlainString() + ".\nThe PDF is attached.\n\nSmart Water Management";
        for (User u : residentsOf(inv.getHousehold().getId())) {
            send(u.getEmail(), "Invoice " + inv.getInvoiceNumber() + " is available", body, inv.getInvoiceNumber() + ".pdf", pdf);
        }
    }

    public void sendAlert(Alert alert) {
        String subject = switch (alert.getAlertType()) {
            case ABNORMAL_USAGE -> "Potential abnormal water usage detected";
            case POTENTIAL_LEAKAGE -> "Potential water leakage pattern detected";
            case THRESHOLD_EXCEEDED -> "Daily water usage threshold exceeded";
            case HIGH_CONSUMPTION -> "High water consumption detected";
        };
        String body = "Hello,\n\n" + alert.getMessage() + "\n\nFlat: " + alert.getHousehold().getFlatNumber()
                + "\nDate: " + alert.getUsageDate()
                + "\n\nThis is an automated notice based on your meter readings. It does not confirm a leak - please check your taps and fixtures.\n\nSmart Water Management";
        for (User u : residentsOf(alert.getHousehold().getId())) {
            send(u.getEmail(), subject, body, null, null);
        }
    }

    public void sendTips(User user, List<WaterSavingTip> tips) {
        StringBuilder sb = new StringBuilder("Hello " + user.getName() + ",\n\nHere are some water-saving tips for this week:\n\n");
        for (WaterSavingTip t : tips) {
            sb.append("* ").append(t.getTitle()).append(": ").append(t.getContent()).append("\n");
        }
        sb.append("\nSmart Water Management");
        send(user.getEmail(), "Your weekly water-saving tips", sb.toString(), null, null);
    }
}

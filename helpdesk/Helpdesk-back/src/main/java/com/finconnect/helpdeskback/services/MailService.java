package com.finconnect.helpdeskback.services;

import com.finconnect.helpdeskback.entities.Ticket;
import com.finconnect.helpdeskback.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;

@Service
public class MailService {
    private final JavaMailSender mailSender;
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Value("${app.mail.from:no-reply@helpdesk.local}")
    private String from;
    @Value("${app.mail.fromName:HelpDesk}")
    private String fromName;

    @Value("${app.mail.to:}")
    private String defaultTo;

    @Value("${app.mail.cc:}")
    private String defaultCc;

    @Value("${app.mail.includeOwnerEmail:true}")
    private boolean includeOwnerEmail;

    @Value("${app.mail.enabled:true}")
    private boolean enabled;

    private final Environment env;

    public MailService(JavaMailSender mailSender, Environment env) {
        this.mailSender = mailSender;
        this.env = env;
    }

    @PostConstruct
    void logMailSetup() {
        try {
            String host = env.getProperty("spring.mail.host", "");
            String user = env.getProperty("spring.mail.username", "");
            String pwd = env.getProperty("spring.mail.password", "");
            boolean pwdSet = pwd != null && !pwd.isBlank();
            log.info("Mail setup => host={}, username={}, passwordSet={}, from={}, to={}, cc={}, includeOwnerEmail={}, enabled={}",
                    host, user, pwdSet, from, defaultTo, defaultCc, includeOwnerEmail, enabled);
        } catch (Exception e) {
            log.warn("Could not log mail setup", e);
        }
    }

    public void sendTicketCreated(Ticket t) {
        send(buildSubjectCreated(t), buildBody("créé", t), t, null);
    }

    public void sendTicketUpdated(Ticket t) {
        send(buildSubjectUpdated(t), buildBody("modifié", t), t, null);
    }

    // Overloads that prioritize sending to the actor (connected user)
    public void sendTicketCreated(Ticket t, String actorEmail) {
        send(buildSubjectCreated(t), buildBody("créé", t), t, actorEmail);
    }

    public void sendTicketUpdated(Ticket t, String actorEmail) {
        send(buildSubjectUpdated(t), buildBody("modifié", t), t, actorEmail);
    }

    private void send(String subject, String html, Ticket t, String actorEmail) {
        try {
            if (!enabled) {
                log.warn("Mail disabled (app.mail.enabled=false). Skipping send: {}", subject);
                return;
            }
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            try {
                helper.setFrom(from, fromName);
            } catch (Exception e) {
                helper.setFrom(from);
            }
            boolean hasRecipient = false;
            if (actorEmail != null && !actorEmail.isBlank()) {
                helper.addTo(actorEmail.trim());
                hasRecipient = true;
            }
            if (defaultTo != null && !defaultTo.isBlank()) {
                for (String to : defaultTo.split(",")) {
                    if (!to.trim().isEmpty()) {
                        helper.addTo(to.trim());
                        hasRecipient = true;
                    }
                }
            }
            if (defaultCc != null && !defaultCc.isBlank()) {
                for (String cc : defaultCc.split(",")) {
                    if (!cc.trim().isEmpty()) {
                        helper.addCc(cc.trim());
                        hasRecipient = true;
                    }
                }
            }
            if (includeOwnerEmail && t != null && t.getUser() != null && t.getUser().getEmail() != null && !t.getUser().getEmail().isBlank()) {
                helper.addCc(t.getUser().getEmail());
                hasRecipient = true;
            }
            if (!hasRecipient) {
                log.warn("No recipients configured (app.mail.to/app.mail.cc empty and owner email missing). Skipping send: {}", subject);
                return;
            }
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
            log.info("Mail sent: {}", subject);
        } catch (Exception ignored) {
            log.error("Failed to send mail: {}", subject, ignored);
        }
    }

    private String buildSubjectCreated(Ticket t) {
        String code = formatCode(t.getId());
        return "Ticket #[" + code + "] créé";
    }

    private String buildSubjectUpdated(Ticket t) {
        String code = formatCode(t.getId());
        return "Ticket #[" + code + "] a été modifié";
    }

    private String buildBody(String action, Ticket t) {
        User u = t.getUser();
        String agent = (u != null ? (u.getUsername() != null ? u.getUsername() : "") : "");
        String code = formatCode(t.getId());
        String statut = t.getStatus() != null ? t.getStatus().name() : "";
        String created = t.getCreatedAt() != null ? t.getCreatedAt().toString() : "";
        String desc = t.getDescription() != null ? t.getDescription() : "";
        String title = t.getTitle() != null ? t.getTitle() : "";
        // Minimal HTML following the screenshot style (French labels)
        return "" +
            "<div style='font-family:Arial,sans-serif;max-width:680px;margin:0 auto'>" +
            "<h2 style='color:#6c2bd9;margin:16px 0'>Ticket #[[" + code + "]] a été " + action + "</h2>" +
            "<p>Bonjour,</p>" +
            "<p>Veuillez trouver les détails de votre demande ci-dessous.</p>" +
            "<table style='width:100%;border-collapse:collapse'>" +
            row("CODE TICKET", code) +
            row("STATUT", statut) +
            row("SUJET", title) +
            row("DESCRIPTION", escape(desc).replace("\n", "<br/>") ) +
            row("AGENT", agent) +
            row("DATE DE CRÉATION", created) +
            "</table>" +
            "</div>";
    }

    private String row(String k, String v) {
        return "<tr><td style='padding:6px 8px;color:#666;width:30%'><b>" + k +
               "</b></td><td style='padding:6px 8px'>" + (v == null ? "" : v) + "</td></tr>";
    }

    private String formatCode(Long id) {
        if (id == null) return "TCK-?????";
        return String.format("TCK-%05d", id);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}

package com.finconnect.helpdeskback.controllers;

import com.finconnect.helpdeskback.entities.Ticket;
import com.finconnect.helpdeskback.entities.TicketStatus;
import com.finconnect.helpdeskback.entities.User;
import com.finconnect.helpdeskback.services.MailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/util")
public class MailTestController {
    private final MailService mailService;

    public MailTestController(MailService mailService) {
        this.mailService = mailService;
    }

    @PostMapping("/mail-test")
    public ResponseEntity<?> sendTest(@RequestParam(defaultValue = "created") String type) {
        Ticket t = new Ticket();
        t.setId(56964L);
        t.setTitle("Campagne Type CallBot");
        t.setDescription("Exemple de description pour test email.");
        t.setStatus(TicketStatus.IN_PROGRESS);
        User u = new User();
        u.setUsername("Nizar TLILI");
        u.setEmail("test-owner@example.com");
        t.setUser(u);
        if ("updated".equalsIgnoreCase(type)) {
            mailService.sendTicketUpdated(t);
        } else {
            mailService.sendTicketCreated(t);
        }
        return ResponseEntity.ok().build();
    }
}

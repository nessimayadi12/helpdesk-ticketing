package com.finconnect.helpdeskback.security;

import com.finconnect.helpdeskback.entities.Ticket;
import com.finconnect.helpdeskback.repositories.TicketRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("ticketSecurity")
public class TicketSecurity {

    private final TicketRepository ticketRepository;

    public TicketSecurity(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public boolean isOwner(Long ticketId) {
        if (ticketId == null) return false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return false;
        String username = auth.getName();
        return ticketRepository.findById(ticketId)
                .map(Ticket::getUser)
                .map(u -> u != null && username.equals(u.getUsername()))
                .orElse(false);
    }
}

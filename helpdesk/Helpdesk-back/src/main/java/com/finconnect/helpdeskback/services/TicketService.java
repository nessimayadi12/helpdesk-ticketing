package com.finconnect.helpdeskback.services;

import com.finconnect.helpdeskback.entities.Ticket;
import com.finconnect.helpdeskback.repositories.TicketRepository;
import com.finconnect.helpdeskback.repositories.UserRepository;
import com.finconnect.helpdeskback.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailService mailService;

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public List<Ticket> getTicketsForCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : null;
        if (username == null) return List.of();
        return ticketRepository.findByUser_Username(username);
    }

    public Optional<Ticket> getTicketById(Long id) {
        return ticketRepository.findById(id);
    }

    public Ticket createTicket(Ticket ticket) {
        // Attach current authenticated user if not provided
        if (ticket.getUser() == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                String username = auth.getName();
                if (username != null) {
                    User user = userRepository.findByUsername(username);
                    if (user != null) {
                        ticket.setUser(user);
                    }
                }
            }
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actorEmail = null;
        if (auth != null) {
            String username = auth.getName();
            if (username != null) {
                User actor = userRepository.findByUsername(username);
                actorEmail = actor != null ? actor.getEmail() : null;
            }
        }
        Ticket saved = ticketRepository.save(ticket);
        try { mailService.sendTicketCreated(saved, actorEmail); } catch (Exception ignored) {}
    return saved;
    }

    public Ticket updateTicket(Long id, Ticket ticketDetails) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new RuntimeException("Ticket non trouvé"));
        ticket.setTitle(ticketDetails.getTitle());
        ticket.setDescription(ticketDetails.getDescription());
        ticket.setStatus(ticketDetails.getStatus());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actorEmail = null;
        if (auth != null) {
            String username = auth.getName();
            if (username != null) {
                User actor = userRepository.findByUsername(username);
                actorEmail = actor != null ? actor.getEmail() : null;
            }
        }
        Ticket saved = ticketRepository.save(ticket);
        try { mailService.sendTicketUpdated(saved, actorEmail); } catch (Exception ignored) {}
    return saved;
    }

    public void deleteTicket(Long id) {
        ticketRepository.deleteById(id);
    }
}

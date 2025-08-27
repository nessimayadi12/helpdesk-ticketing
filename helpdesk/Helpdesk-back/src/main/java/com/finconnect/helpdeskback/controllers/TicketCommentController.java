package com.finconnect.helpdeskback.controllers;

import com.finconnect.helpdeskback.entities.Ticket;
import com.finconnect.helpdeskback.entities.TicketComment;
import com.finconnect.helpdeskback.entities.User;
import com.finconnect.helpdeskback.repositories.TicketCommentRepository;
import com.finconnect.helpdeskback.repositories.TicketRepository;
import com.finconnect.helpdeskback.repositories.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
public class TicketCommentController {

    private final TicketCommentRepository commentRepo;
    private final TicketRepository ticketRepo;
    private final UserRepository userRepo;
    private final SseController sse;

    public TicketCommentController(TicketCommentRepository commentRepo, TicketRepository ticketRepo, UserRepository userRepo, SseController sse) {
        this.commentRepo = commentRepo;
        this.ticketRepo = ticketRepo;
        this.userRepo = userRepo;
        this.sse = sse;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<TicketComment> list(@PathVariable Long ticketId) {
        return commentRepo.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public List<com.finconnect.helpdeskback.dto.CommentStats> stats(@RequestParam List<Long> ids) {
        // For each ticket id, fetch last comment (simple approach; can be optimized with a query)
        return ids.stream().map(tid -> {
            List<TicketComment> all = commentRepo.findByTicketIdOrderByCreatedAtAsc(tid);
            if (all.isEmpty()) return new com.finconnect.helpdeskback.dto.CommentStats(tid, null, null);
            TicketComment last = all.get(all.size()-1);
            return new com.finconnect.helpdeskback.dto.CommentStats(tid, last.getId(), last.getCreatedAt());
        }).collect(Collectors.toList());
    }

    public static class CreateCommentDto { public String content; }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public TicketComment add(@PathVariable Long ticketId, @RequestBody CreateCommentDto dto) {
        Ticket ticket = ticketRepo.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket non trouvé"));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        if (username == null) throw new RuntimeException("Utilisateur non authentifié");
        User user = userRepo.findByUsername(username);
        if (user == null) throw new RuntimeException("Utilisateur non trouvé");
        TicketComment c = new TicketComment();
        c.setTicket(ticket);
        c.setUser(user);
        c.setContent(dto.content);
    TicketComment saved = commentRepo.save(c);
    // Broadcast SSE event
    com.finconnect.helpdeskback.dto.CommentEvent ev = new com.finconnect.helpdeskback.dto.CommentEvent(
        ticket.getId(), saved.getId(), saved.getContent(), user.getUsername(), saved.getCreatedAt()
    );
    sse.broadcast(ev);
    return saved;
    }
}

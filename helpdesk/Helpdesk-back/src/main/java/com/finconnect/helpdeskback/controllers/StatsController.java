package com.finconnect.helpdeskback.controllers;

import com.finconnect.helpdeskback.entities.Ticket;
import com.finconnect.helpdeskback.entities.TicketStatus;
import com.finconnect.helpdeskback.entities.User;
import com.finconnect.helpdeskback.repositories.TicketRepository;
import com.finconnect.helpdeskback.repositories.UserRepository;
import com.finconnect.helpdeskback.repositories.TicketCommentRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.time.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final TicketRepository ticketRepo;
        private final UserRepository userRepo;
        private final TicketCommentRepository commentRepo;

        public StatsController(TicketRepository ticketRepo, UserRepository userRepo, TicketCommentRepository commentRepo) {
                this.ticketRepo = ticketRepo;
                this.userRepo = userRepo;
                this.commentRepo = commentRepo;
        }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> adminStats() {
        List<Ticket> all = ticketRepo.findAll();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("totalTickets", all.size());
        Map<String, Long> byStatus = Arrays.stream(TicketStatus.values())
                .collect(Collectors.toMap(Enum::name, st -> all.stream().filter(t -> st.equals(t.getStatus())).count()));
        res.put("byStatus", byStatus);
        // per user counts
        Map<String, Long> byUser = all.stream()
                .collect(Collectors.groupingBy(t -> t.getUser() != null ? t.getUser().getUsername() : "(inconnu)", Collectors.counting()));
        res.put("byUser", byUser);
        // 5 latest tickets
                List<Map<String,Object>> latest = all.stream()
                                .sorted(Comparator.comparing(Ticket::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                                .limit(5)
                                .map(t -> {
                                        Map<String,Object> m = new LinkedHashMap<>();
                                        m.put("id", t.getId());
                                        m.put("title", t.getTitle());
                                        m.put("status", t.getStatus() != null ? t.getStatus().name() : null);
                                        m.put("user", t.getUser() != null ? t.getUser().getUsername() : null);
                                        m.put("createdAt", t.getCreatedAt());
                                        return m;
                                })
                                .collect(Collectors.toList());
        res.put("latest", latest);
        return res;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Map<String,Object> myStats() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        User user = username != null ? userRepo.findByUsername(username) : null;
        List<Ticket> mine = user != null ? ticketRepo.findAll().stream()
                .filter(t -> t.getUser() != null && username.equals(t.getUser().getUsername()))
                .collect(Collectors.toList()) : Collections.emptyList();
                Map<String, Object> res = new LinkedHashMap<>();
                res.put("myTickets", mine.size());

                Map<String, Long> byStatus = Arrays.stream(TicketStatus.values())
                                .collect(Collectors.toMap(Enum::name, st -> mine.stream().filter(t -> st.equals(t.getStatus())).count()));
                res.put("byStatus", byStatus);

                // Latest created tickets (5)
                List<Map<String,Object>> latest = mine.stream()
                                .sorted(Comparator.comparing(Ticket::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                                .limit(5)
                                .map(t -> {
                                        Map<String,Object> m = new LinkedHashMap<>();
                                        m.put("id", t.getId());
                                        m.put("title", t.getTitle());
                                        m.put("status", t.getStatus() != null ? t.getStatus().name() : null);
                                        m.put("createdAt", t.getCreatedAt());
                                        return m;
                                })
                                .collect(Collectors.toList());
                res.put("latest", latest);

                // Recently updated tickets (5)
                List<Map<String,Object>> recentUpdated = mine.stream()
                                .sorted(Comparator.comparing(Ticket::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                                .limit(5)
                                .map(t -> {
                                        Map<String,Object> m = new LinkedHashMap<>();
                                        m.put("id", t.getId());
                                        m.put("title", t.getTitle());
                                        m.put("status", t.getStatus() != null ? t.getStatus().name() : null);
                                        m.put("updatedAt", t.getUpdatedAt());
                                        return m;
                                })
                                .collect(Collectors.toList());
                res.put("recentUpdated", recentUpdated);

                // Created in last 7 days (by day label)
                ZoneId zone = ZoneId.systemDefault();
                LocalDate today = LocalDate.now(zone);
                LocalDate start = today.minusDays(6);
                Map<String, Long> recentCreated = new LinkedHashMap<>();
                for (int i = 0; i < 7; i++) {
                        LocalDate dte = start.plusDays(i);
                        recentCreated.put(dte.toString(), 0L);
                }
                mine.forEach(t -> {
                        Instant created = t.getCreatedAt();
                        if (created != null) {
                                LocalDate d = created.atZone(zone).toLocalDate();
                                if (!d.isBefore(start) && !d.isAfter(today)) {
                                        String key = d.toString();
                                        recentCreated.computeIfPresent(key, (k, v) -> v + 1);
                                }
                        }
                });
                res.put("recentCreated", recentCreated);

                // Comments summary for my tickets
                List<Long> ids = mine.stream().map(Ticket::getId).collect(Collectors.toList());
                Map<Long, Long> commentCounts = new HashMap<>();
                if (!ids.isEmpty()) {
                        List<Object[]> rows = commentRepo.countByTicketIds(ids);
                        for (Object[] r : rows) {
                                Long tid = (Long) r[0];
                                Long cnt = (Long) r[1];
                                commentCounts.put(tid, cnt);
                        }
                }
                long totalComments = commentCounts.values().stream().mapToLong(Long::longValue).sum();
                List<Map<String,Object>> topCommented = mine.stream()
                                .map(t -> {
                                        Map<String,Object> m = new LinkedHashMap<>();
                                        m.put("id", t.getId());
                                        m.put("title", t.getTitle());
                                        m.put("count", commentCounts.getOrDefault(t.getId(), 0L));
                                        return m;
                                })
                                .sorted((a,b) -> Long.compare((Long)b.get("count"), (Long)a.get("count")))
                                .limit(5)
                                .collect(Collectors.toList());

                Map<String,Object> comments = new LinkedHashMap<>();
                comments.put("total", totalComments);
                comments.put("top", topCommented);
                res.put("comments", comments);

                return res;
    }
}

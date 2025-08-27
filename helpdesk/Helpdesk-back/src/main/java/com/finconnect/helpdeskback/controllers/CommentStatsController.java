package com.finconnect.helpdeskback.controllers;

import com.finconnect.helpdeskback.dto.CommentStats;
import com.finconnect.helpdeskback.entities.TicketComment;
import com.finconnect.helpdeskback.repositories.TicketCommentRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comments")
public class CommentStatsController {

    private final TicketCommentRepository commentRepo;

    public CommentStatsController(TicketCommentRepository commentRepo) {
        this.commentRepo = commentRepo;
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public List<CommentStats> stats(@RequestParam List<Long> ids) {
        return ids.stream().map(tid -> {
            List<TicketComment> all = commentRepo.findByTicketIdOrderByCreatedAtAsc(tid);
            if (all.isEmpty()) return new CommentStats(tid, null, null);
            TicketComment last = all.get(all.size() - 1);
            return new CommentStats(tid, last.getId(), last.getCreatedAt());
        }).collect(Collectors.toList());
    }
}

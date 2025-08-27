package com.finconnect.helpdeskback.dto;

import java.time.Instant;

public class CommentStats {
    public Long ticketId;
    public Long lastCommentId;
    public Instant lastCommentAt;

    public CommentStats(Long ticketId, Long lastCommentId, Instant lastCommentAt) {
        this.ticketId = ticketId;
        this.lastCommentId = lastCommentId;
        this.lastCommentAt = lastCommentAt;
    }
}

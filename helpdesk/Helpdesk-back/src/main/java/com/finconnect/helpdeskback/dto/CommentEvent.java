package com.finconnect.helpdeskback.dto;

import java.time.Instant;

public class CommentEvent {
    public Long ticketId;
    public Long commentId;
    public String content;
    public String authorUsername;
    public Instant createdAt;

    public CommentEvent() {}
    public CommentEvent(Long ticketId, Long commentId, String content, String authorUsername, Instant createdAt) {
        this.ticketId = ticketId;
        this.commentId = commentId;
        this.content = content;
        this.authorUsername = authorUsername;
        this.createdAt = createdAt;
    }
}

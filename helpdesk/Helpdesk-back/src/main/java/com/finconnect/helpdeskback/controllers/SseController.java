package com.finconnect.helpdeskback.controllers;

import com.finconnect.helpdeskback.dto.CommentEvent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@RestController
@RequestMapping("/api/sse")
public class SseController {
    private final Set<SseEmitter> clients = new CopyOnWriteArraySet<>();

    @GetMapping(path = "/comments", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamComments() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        clients.add(emitter);
        emitter.onCompletion(() -> clients.remove(emitter));
        emitter.onTimeout(() -> clients.remove(emitter));
        return emitter;
    }

    public void broadcast(CommentEvent event) {
        clients.forEach(em -> {
            try {
                em.send(SseEmitter.event().name("comment").data(event));
            } catch (IOException e) {
                em.complete();
                clients.remove(em);
            }
        });
    }
}

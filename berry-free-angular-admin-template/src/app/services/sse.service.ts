import { Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

export interface CommentEvent {
  ticketId: number;
  commentId: number;
  content: string;
  authorUsername: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class SseService {
  constructor(private zone: NgZone, private auth: AuthService) {}

  comments$(): Observable<CommentEvent> {
    return new Observable<CommentEvent>((observer) => {
      let es: EventSource | null = null;
      let closed = false;

      const connect = () => {
        const token = this.auth.getToken();
        const url = token
          ? `http://localhost:8084/api/sse/comments?token=${encodeURIComponent(token)}`
          : 'http://localhost:8084/api/sse/comments';
        es = new EventSource(url, { withCredentials: true } as any);
        es.addEventListener('comment', (e: MessageEvent) => {
          this.zone.run(() => observer.next(JSON.parse(e.data)));
        });
        es.onerror = () => {
          try { es && es.close(); } catch {}
          if (closed) return;
          // Reconnect after a short delay
          setTimeout(connect, 2000);
        };
      };

      connect();

      return () => {
        closed = true;
        try { es && es.close(); } catch {}
      };
    });
  }
}

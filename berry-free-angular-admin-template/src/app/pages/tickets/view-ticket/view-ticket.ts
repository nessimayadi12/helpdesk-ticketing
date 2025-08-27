import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Ticket, TicketService } from 'src/app/services/ticket';
import { NotificationService } from 'src/app/theme/shared/service/notification.service';
import { SseService } from 'src/app/services/sse.service';

interface TicketComment {
  id: number;
  content: string;
  createdAt: string;
  user?: { id?: number; username?: string; email?: string };
}

@Component({
  selector: 'app-view-ticket',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './view-ticket.html',
  styleUrls: ['./view-ticket.scss']
})
export class ViewTicketComponent implements OnInit, OnDestroy {
  ticket?: Ticket;
  comments: TicketComment[] = [];
  newComment = '';
  private pollHandle: any;
  private lastCommentCount = 0;
  private initialMarked = false;

  constructor(private route: ActivatedRoute, private ticketService: TicketService, private http: HttpClient, private notif: NotificationService, private sse: SseService) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.ticketService.getTicket(id).subscribe((t) => this.ticket = t);
      // Initial load: mark seen once on entering the page
      this.loadComments(id, { markSeen: true });
      // Listen to SSE for new comments
      this.sse.comments$().subscribe(ev => {
        if (ev.ticketId === id) {
          this.notif.info('Nouveau commentaire: ' + (ev.content?.slice(0, 60) || ''));
          // Refresh comments
          this.loadComments(id, { markSeen: false });
        }
      });
    }
  }

  ngOnDestroy(): void {
    if (this.pollHandle) clearInterval(this.pollHandle);
  }

  loadComments(id: number, opts?: { markSeen?: boolean }) {
    this.http.get<TicketComment[]>(`http://localhost:8084/api/tickets/${id}/comments`).subscribe((cs) => {
      this.comments = cs;
      this.lastCommentCount = cs.length;
      // Mark last comment as seen only on initial load or explicit request
      if (opts?.markSeen) {
        try {
          const last = cs[cs.length - 1];
          const store = JSON.parse(localStorage.getItem('ticketLastSeen') || '{}');
          const current = store[id] || 0;
          const next = Math.max(current, last?.id || 0);
          store[id] = next;
          localStorage.setItem('ticketLastSeen', JSON.stringify(store));
        } catch {}
      }
    });
  }

  // SSE now handles real-time updates; no polling method needed.

  addComment() {
    const id = this.ticket?.id;
    if (!id || !this.newComment.trim()) return;
    this.http.post<TicketComment>(`http://localhost:8084/api/tickets/${id}/comments`, { content: this.newComment.trim() })
      .subscribe({
  next: () => { this.newComment = ''; this.loadComments(id, { markSeen: true }); this.notif.success('Commentaire ajouté.'); },
        error: (e) => { console.error('Erreur ajout commentaire', e); this.notif.error('Échec de l\'ajout du commentaire'); }
      });
  }
}

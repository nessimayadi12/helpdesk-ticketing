import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Ticket, TicketService } from 'src/app/services/ticket';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService } from 'src/app/services/auth.service';
import { SseService } from 'src/app/services/sse.service';
import { NotificationService } from 'src/app/theme/shared/service/notification.service';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-list-ticket',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './list-ticket.html',
  styleUrls: ['./list-ticket.scss']
})
export class ListTicketComponent implements OnInit, OnDestroy {
  tickets: Ticket[] = [];
  filteredTickets: Ticket[] = [];
  isAdmin = false;
  lastSeen: Record<number, number> = {};
  private statsTimer: any;
  // Filters
  search: string = '';
  status: 'ALL' | 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' = 'ALL';
  readonly statusOptions: Array<{ value: 'ALL' | 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'; label: string }> = [
    { value: 'ALL', label: 'Tous' },
    { value: 'OPEN', label: 'Ouvert' },
    { value: 'IN_PROGRESS', label: 'En cours' },
    { value: 'RESOLVED', label: 'Résolu' },
    { value: 'CLOSED', label: 'Fermé' }
  ];

  constructor(
    private ticketService: TicketService,
    private router: Router,
    private auth: AuthService,
    private sse: SseService,
    private notif: NotificationService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.auth.getRole() === 'ADMIN';
  // Load last seen map from localStorage
  try { this.lastSeen = JSON.parse(localStorage.getItem('ticketLastSeen') || '{}'); } catch {}
  this.loadTickets();
  // Periodic stats refresh as a fallback (every 15s)
  this.statsTimer = setInterval(() => this.refreshStats(), 5000);
    // Live updates: when a new comment arrives, update the corresponding ticket
    this.sse.comments$().subscribe((ev) => {
      // debug: confirm we receive SSE
      try { console.debug('[SSE] comment event', ev); } catch {}
      const idx = this.tickets.findIndex(x => x.id === ev.ticketId);
      if (idx === -1) return;
  const currentUser = this.auth.getUsername();
      const isSelf = !!(ev.authorUsername && currentUser && ev.authorUsername.toLowerCase() === currentUser.toLowerCase());
      const prev = this.tickets[idx] as any;
      const newLast = Math.max(prev.lastCommentId || 0, ev.commentId || 0);
      const updated = { ...this.tickets[idx], lastCommentId: newLast } as Ticket;
      this.tickets = [
        ...this.tickets.slice(0, idx),
        updated,
        ...this.tickets.slice(idx + 1)
      ];
  this.applyFilters();
      if (isSelf) {
        this.markSeen(ev.ticketId, ev.commentId);
        return;
      }
      if ((this.lastSeen[ev.ticketId] || 0) < newLast) {
        this.notif.info(`Nouveau commentaire sur le ticket #${ev.ticketId}`);
        try { console.debug('[Unread] dot should light for ticket', ev.ticketId, 'lastSeen=', this.lastSeen[ev.ticketId] || 0, 'last=', newLast); } catch {}
      }
    });
  }

  ngOnDestroy(): void {
    if (this.statsTimer) clearInterval(this.statsTimer);
  }

  private refreshStats() {
    const ids = this.tickets.map(t => t.id).filter((v): v is number => typeof v === 'number');
    if (!ids.length) return;
    const params = new URLSearchParams();
    ids.forEach(id => params.append('ids', String(id)));
    this.http.get<Array<{ ticketId: number; lastCommentId: number | null }>>(`http://localhost:8084/api/comments/stats?${params.toString()}`)
      .subscribe({
        next: (stats) => {
          const mapStats = new Map(stats.map(s => [s.ticketId, s.lastCommentId || 0]));
          // Notify for unseen increments
          for (const t of this.tickets) {
            const id = t.id as number;
            const prevLast = (t as any).lastCommentId || 0;
            const newLast = mapStats.get(id) ?? prevLast;
            if (newLast > prevLast && newLast > (this.lastSeen[id] || 0)) {
              try { this.notif.info(`Nouveau commentaire sur le ticket #${id}`); } catch {}
            }
          }
          this.tickets = this.tickets.map(t => ({
            ...t,
            lastCommentId: (mapStats.get(t.id as number) ?? ((t as any).lastCommentId || 0))
          }));
          try { console.debug('[Stats] refreshed', stats); } catch {}
          this.applyFilters();
        },
        error: () => { /* silent */ }
      });
  }

  loadTickets() {
  const source$ = this.isAdmin ? this.ticketService.getTickets() : this.ticketService.getMyTickets();
    source$.subscribe({
      next: (data) => { this.tickets = data; this.applyFilters(); this.refreshStats(); },
      error: (err) => {
        if (err?.status === 403) {
          this.tickets = [];
          this.filteredTickets = [];
          return;
        }
        console.error('Erreur lors du chargement des tickets', err);
      }
    });
  }

  applyFilters() {
    const s = (this.search || '').trim().toLowerCase();
    const status = this.status;
    const matchesSearch = (t: Ticket) => {
      if (!s) return true;
      const idStr = (t.id != null ? String(t.id) : '');
      const title = (t.title || '').toLowerCase();
      const desc = (t.description || '').toLowerCase();
      const owner = (this.getOwnerName(t) || '').toLowerCase();
      return idStr.includes(s) || title.includes(s) || desc.includes(s) || owner.includes(s);
    };
    const matchesStatus = (t: Ticket) => status === 'ALL' || t.status === status;
    this.filteredTickets = this.tickets.filter(t => matchesStatus(t) && matchesSearch(t));
  }

  resetFilters() {
    this.search = '';
    this.status = 'ALL';
    this.applyFilters();
  }

  getOwnerName(t: Ticket): string {
    const anyT: any = t as any;
    const direct = t.ownerUsername || t.createdBy || t.owner;
    if (direct) return String(direct);
    const userObj = anyT.user || anyT.createdBy || anyT.owner;
    if (userObj && typeof userObj === 'object') {
      return userObj.username || userObj.name || userObj.email || '-';
    }
    return '-';
  }

  getOwnerId(t: Ticket): string | number {
    const anyT: any = t as any;
    if (t.userId != null) return t.userId;
    const userObj = anyT.user || anyT.createdBy || anyT.owner;
    if (userObj && typeof userObj === 'object') {
      return userObj.id ?? '-';
    }
    return '-';
  }

  onAddTicket() {
    this.router.navigate(['/ajout-ticket']);
  }

  onEditTicket(ticket: Ticket) {
    this.router.navigate(['/modifier-ticket', ticket.id]);
  }

  markSeen(ticketId: number, commentId?: number) {
    if (ticketId == null) return;
    const current = this.lastSeen[ticketId] || 0;
    const next = Math.max(current, commentId || current);
    this.lastSeen[ticketId] = next;
    localStorage.setItem('ticketLastSeen', JSON.stringify(this.lastSeen));
  }

  onDeleteTicket(id: number) {
    if (confirm('Voulez-vous vraiment supprimer ce ticket ?')) {
      this.ticketService.deleteTicket(id).subscribe({
        next: () => {
          this.notif.success('Ticket supprimé');
          this.loadTickets();
        },
        error: (err) => {
          if (err?.status === 403) {
            this.notif.error("Action non autorisée: vous ne pouvez supprimer que vos propres tickets.");
          } else if (err?.status === 401) {
            this.notif.error("Session expirée. Veuillez vous reconnecter.");
          } else {
            this.notif.error("Erreur lors de la suppression");
          }
          console.error('Erreur lors de la suppression', err);
        }
      });
    }
  }

  isOwner(t: Ticket): boolean {
    const me = (this.auth.getUsername() || '').toLowerCase();
    const anyT: any = t as any;
    const direct = (t.ownerUsername || t.createdBy || t.owner || '') as string;
    const fromObj = anyT.user && (anyT.user.username || anyT.user.name || anyT.user.email);
    const owner = (direct || fromObj || '').toString().toLowerCase();
    return !!me && !!owner && me === owner;
  }

  canManage(t: Ticket): boolean {
    return this.isAdmin || this.isOwner(t);
  }
}

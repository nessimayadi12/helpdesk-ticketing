import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Ticket, TicketService } from 'src/app/services/ticket';
import { CommonModule } from '@angular/common';
import { AuthService } from 'src/app/services/auth.service';

@Component({
  selector: 'app-list-ticket',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './list-ticket.html',
  styleUrls: ['./list-ticket.scss']
})
export class ListTicketComponent implements OnInit {
  tickets: Ticket[] = [];
  isAdmin = false;

  constructor(private ticketService: TicketService, private router: Router, private auth: AuthService) {}

  ngOnInit(): void {
    this.isAdmin = this.auth.getRole() === 'ADMIN';
    this.loadTickets();
  }

  loadTickets() {
  const source$ = this.isAdmin ? this.ticketService.getTickets() : this.ticketService.getMyTickets();
    source$.subscribe({
      next: (data) => this.tickets = data,
      error: (err) => {
        if (err?.status === 403) {
          this.tickets = [];
          return;
        }
        console.error('Erreur lors du chargement des tickets', err);
      }
    });
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

  onDeleteTicket(id: number) {
    if (confirm('Voulez-vous vraiment supprimer ce ticket ?')) {
      this.ticketService.deleteTicket(id).subscribe({
        next: () => this.loadTickets(),
        error: (err) => console.error('Erreur lors de la suppression', err)
      });
    }
  }
}

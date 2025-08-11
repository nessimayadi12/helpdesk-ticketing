import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Ticket, TicketService } from 'src/app/services/ticket';

@Component({
  selector: 'app-add-ticket',
  standalone: true,
  templateUrl: './add-ticket.html',
  styleUrls: ['./add-ticket.scss'],
  imports: [CommonModule, FormsModule, RouterModule]
})
export class AddTicketComponent {
  ticket: Ticket = { title: '', description: '', status: 'OPEN' };
  errorMessage: string | null = null;

  constructor(private ticketService: TicketService, private router: Router) {}

  onSubmit() {
    this.errorMessage = null;
    // Send only allowed fields; backend will set the current user as owner
    const payload: Ticket = {
      title: this.ticket.title,
      description: this.ticket.description,
      status: this.ticket.status
    };
    this.ticketService.addTicket(payload).subscribe({
      next: () => this.router.navigate(['/tickets']),
      error: () => this.errorMessage = 'Erreur lors de l\'ajout du ticket'
    });
  }
}

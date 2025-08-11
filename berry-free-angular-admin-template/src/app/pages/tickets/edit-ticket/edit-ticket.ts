import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Ticket, TicketService } from 'src/app/services/ticket';

@Component({
  selector: 'app-edit-ticket',
  templateUrl: './edit-ticket.html',
  styleUrls: ['./edit-ticket.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule]
})
export class EditTicketComponent implements OnInit {
  ticket: Ticket = { title: '', description: '', status: 'OPEN' };
  errorMessage: string | null = null;

  constructor(
    private ticketService: TicketService, 
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.ticketService.getTicket(id).subscribe({
      next: (data) => this.ticket = data,
      error: () => this.errorMessage = 'Ticket introuvable'
    });
  }

  onSubmit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.ticketService.updateTicket(id, this.ticket).subscribe({
      next: () => this.router.navigate(['/tickets']),
      error: () => this.errorMessage = 'Erreur lors de la modification du ticket'
    });
  }
}

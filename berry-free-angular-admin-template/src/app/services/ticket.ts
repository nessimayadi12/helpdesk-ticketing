import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

export interface Ticket {
  id?: number;
  title: string;
  description: string;
  status: 'OPEN' | 'IN_PROGRESS' | 'CLOSED' | 'RESOLVED';
  // Optional ownership fields (depending on backend response shape)
  ownerUsername?: string;
  createdBy?: string;
  owner?: string;
  userId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class TicketService {
  private apiUrl = 'http://localhost:8084/api/tickets';

  constructor(private http: HttpClient, private auth: AuthService) {}

  getTickets(): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(this.apiUrl);
  }

  getTicket(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.apiUrl}/${id}`);
  }

  /**
   * Returns only tickets of the currently logged-in user.
   * Tries preferred endpoints first, then gracefully falls back.
   */
  getMyTickets(): Observable<Ticket[]> {
    const username = this.auth.getUsername();
    if (!username) {
      // No user in session – return empty list
      return of([]);
    }

    // 1) Prefer a dedicated endpoint if backend supports it
    return this.http.get<Ticket[]>(`${this.apiUrl}/my`).pipe(
      catchError(() =>
        // 2) Try query param fallback if supported; if still blocked, return empty list
        this.http.get<Ticket[]>(`${this.apiUrl}?owner=${encodeURIComponent(username)}`).pipe(
          catchError(() => of([]))
        )
      )
    );
  }

  private filterTicketsByUser(tickets: Ticket[], username: string): Ticket[] {
    // Try several common ownership fields
    return tickets.filter((t) => {
      const possible = [t.ownerUsername, t.createdBy, t.owner] as (string | undefined)[];
      // If any known field matches, keep
      if (possible.some((v) => v && v.toLowerCase() === username.toLowerCase())) return true;

      // If object-style ownership exists (rare), attempt to stringify match
      const anyT: any = t as any;
      const userObj = anyT.user || anyT.createdBy || anyT.owner;
      if (userObj && typeof userObj === 'object') {
        const uname = userObj.username || userObj.name || userObj.email;
        if (uname && String(uname).toLowerCase() === username.toLowerCase()) return true;
      }
      return false;
    });
  }

  addTicket(ticket: Ticket): Observable<Ticket> {
    return this.http.post<Ticket>(this.apiUrl, ticket);
  }

  updateTicket(id: number, ticket: Ticket): Observable<Ticket> {
    return this.http.put<Ticket>(`${this.apiUrl}/${id}`, ticket);
  }

  deleteTicket(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

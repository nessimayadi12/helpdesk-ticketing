import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class StatsService {
  private api = `${environment.apiUrl ?? 'http://localhost:8084'}/api/stats`;
  constructor(private http: HttpClient) {}
  admin(): Observable<any> { return this.http.get<any>(`${this.api}/admin`); }
  me(): Observable<any> { return this.http.get<any>(`${this.api}/me`); }
}

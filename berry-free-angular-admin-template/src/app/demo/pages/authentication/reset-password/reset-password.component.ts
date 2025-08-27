import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
  <div class="container py-5" style="max-width:560px">
    <h3 class="mb-3">Réinitialiser le mot de passe</h3>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <div class="mb-3">
        <label class="form-label">Nouveau mot de passe</label>
        <input type="password" class="form-control" formControlName="password" required />
      </div>
      <div class="mb-3">
        <label class="form-label">Confirmer le mot de passe</label>
        <input type="password" class="form-control" formControlName="confirm" required />
      </div>
      <div *ngIf="error" class="alert alert-danger">{{ error }}</div>
      <div *ngIf="success" class="alert alert-success">Mot de passe modifié. Vous pouvez vous connecter.</div>
      <button class="btn btn-primary" type="submit" [disabled]="form.invalid || loading">{{ loading ? 'En cours...' : 'Valider' }}</button>
    </form>
  </div>
  `
})
export default class ResetPasswordComponent {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = environment.apiUrl ?? 'http://localhost:8084';

  token = this.route.snapshot.queryParamMap.get('token');
  form = this.fb.group({ password: ['', [Validators.required, Validators.minLength(6)]], confirm: ['', [Validators.required]] });
  loading = false;
  error: string | null = null;
  success = false;

  submit() {
    if (this.loading) return;
    this.error = null;
    const { password, confirm } = this.form.value as any;
    if (!this.token) { this.error = 'Lien invalide'; return; }
    if (password !== confirm) { this.error = 'Les mots de passe ne correspondent pas'; return; }
    this.loading = true;
    this.http.post(`${this.api}/api/auth/reset-password`, { token: this.token, newPassword: password }).subscribe({
      next: () => { this.loading = false; this.success = true; setTimeout(() => this.router.navigate(['/guest/login']), 1200); },
      error: (err) => { this.loading = false; this.error = err?.error?.message || 'Lien invalide ou expiré'; }
    });
  }
}

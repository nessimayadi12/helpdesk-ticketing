// angular import
import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from 'src/app/services/auth.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterModule, CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export default class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private http = inject(HttpClient);
  private api = environment.apiUrl ?? 'http://localhost:8084';

  form: FormGroup = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });
  loading = false;
  error: string | null = null;
  forgotInfo: string | null = null;

  submit() {
    if (this.form.invalid || this.loading) return;
    this.loading = true;
    this.error = null;
    const { username, password } = this.form.value;
    this.auth.login(username, password).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/default']);
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Erreur de connexion';
      }
    });
  }

  forgot() {
    const identifier = (this.form.get('username')?.value || '').trim();
    if (!identifier) {
      this.error = 'Veuillez saisir votre email ou username dans le champ identifiant.';
      return;
    }
    this.error = null;
    this.forgotInfo = null;
    this.http.post(`${this.api}/api/auth/forgot-password`, { identifier }).subscribe({
      next: () => this.forgotInfo = 'Si un compte existe, un email a été envoyé.',
      error: () => this.forgotInfo = 'Si un compte existe, un email a été envoyé.'
    });
  }
}

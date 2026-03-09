import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  email = '';
  password = '';
  error = signal('');
  loading = signal(false);

  constructor(private router: Router, private auth: AuthService) {}

  onSubmit() {
    this.error.set('');
    this.loading.set(true);
    this.auth.login(this.email, this.password).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.loading.set(false);
        const msg = err?.error?.message ?? err?.error ?? 'Login fehlgeschlagen';
        this.error.set(typeof msg === 'string' ? msg : 'Login fehlgeschlagen');
      }
    });
  }
}

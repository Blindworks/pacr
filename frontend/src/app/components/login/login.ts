import { Component, OnInit, signal } from '@angular/core';
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
export class Login implements OnInit {
  email = '';
  password = '';
  error = signal('');
  loading = signal(false);
  verifiedSuccess = signal(false);

  constructor(private router: Router, private auth: AuthService) {}

  ngOnInit() {
    if (window.history.state?.verified === true) {
      this.verifiedSuccess.set(true);
    }
  }

  onSubmit() {
    this.error.set('');
    this.loading.set(true);
    this.auth.login(this.email, this.password).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.loading.set(false);
        const msg = err?.error?.message ?? err?.error ?? 'Login fehlgeschlagen';
        this.error.set(typeof msg === 'string' ? msg : 'Login fehlgeschlagen');
      }
    });
  }
}

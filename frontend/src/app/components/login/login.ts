import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink, TranslateModule],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login implements OnInit {
  email = '';
  password = '';
  error = signal('');
  loading = signal(false);
  verifiedSuccess = signal(false);

  constructor(private router: Router, private auth: AuthService, private translate: TranslateService) {}

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
        if (err?.status === 403 && err?.error?.status === 'EMAIL_VERIFICATION_PENDING') {
          const email = err?.error?.email ?? this.email;
          this.router.navigate(['/verify-email'], { state: { email } });
          return;
        }
        const msg = err?.error?.message ?? err?.error ?? this.translate.instant('LOGIN.FAILED');
        this.error.set(typeof msg === 'string' ? msg : this.translate.instant('LOGIN.FAILED'));
      }
    });
  }
}

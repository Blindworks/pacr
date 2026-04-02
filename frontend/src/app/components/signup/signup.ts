import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [FormsModule, RouterLink, TranslateModule],
  templateUrl: './signup.html',
  styleUrl: './signup.scss'
})
export class Signup {
  fullName = '';
  username = '';
  email = '';
  password = '';
  showPassword = false;
  loading = signal(false);
  error = signal('');

  constructor(private router: Router, private auth: AuthService, private translate: TranslateService) {}

  onSubmit() {
    this.error.set('');
    this.loading.set(true);
    this.auth.register(this.username, this.email, this.password).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/verify-email'], { state: { email: this.email } });
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err?.error?.message ?? err?.error ?? this.translate.instant('SIGNUP.FAILED');
        this.error.set(typeof msg === 'string' ? msg : this.translate.instant('SIGNUP.FAILED'));
      }
    });
  }
}

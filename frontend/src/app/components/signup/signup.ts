import { Component, signal, computed } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';

export interface PasswordRule {
  key: string;
  met: boolean;
}

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
  passwordInput = signal('');

  passwordRules = computed<PasswordRule[]>(() => {
    const pw = this.passwordInput();
    return [
      { key: 'SIGNUP.PW_RULE_LENGTH', met: pw.length >= 10 },
      { key: 'SIGNUP.PW_RULE_UPPERCASE', met: /[A-Z]/.test(pw) },
      { key: 'SIGNUP.PW_RULE_LOWERCASE', met: /[a-z]/.test(pw) },
      { key: 'SIGNUP.PW_RULE_DIGIT', met: /\d/.test(pw) },
      { key: 'SIGNUP.PW_RULE_SPECIAL', met: /[^A-Za-z0-9]/.test(pw) },
    ];
  });

  passwordScore = computed(() => {
    return this.passwordRules().filter(r => r.met).length;
  });

  passwordStrengthPercent = computed(() => {
    return (this.passwordScore() / 5) * 100;
  });

  passwordStrengthLabel = computed(() => {
    const score = this.passwordScore();
    if (score <= 1) return 'SIGNUP.PW_STRENGTH_WEAK';
    if (score <= 2) return 'SIGNUP.PW_STRENGTH_FAIR';
    if (score <= 3) return 'SIGNUP.PW_STRENGTH_GOOD';
    return 'SIGNUP.PW_STRENGTH_STRONG';
  });

  passwordStrengthColor = computed(() => {
    const score = this.passwordScore();
    if (score <= 1) return '#ef4444';
    if (score <= 2) return '#f97316';
    if (score <= 3) return '#eab308';
    if (score === 4) return '#84cc16';
    return '#22c55e';
  });

  passwordAllRulesMet = computed(() => {
    return this.passwordRules().every(r => r.met);
  });

  constructor(private router: Router, private auth: AuthService, private translate: TranslateService) {}

  onPasswordChange(value: string) {
    this.password = value;
    this.passwordInput.set(value);
  }

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

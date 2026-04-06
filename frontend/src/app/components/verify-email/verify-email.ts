import { Component, OnInit, signal, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [FormsModule, RouterLink, TranslateModule],
  templateUrl: './verify-email.html',
  styleUrl: './verify-email.scss'
})
export class VerifyEmail implements OnInit, OnDestroy {
  email = signal('');
  digits = ['', '', '', '', '', ''];
  loading = signal(false);
  error = signal('');
  resendCooldown = signal(0);
  resendLoading = signal(false);

  private cooldownInterval: ReturnType<typeof setInterval> | null = null;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private auth: AuthService,
    private translate: TranslateService
  ) {}

  ngOnInit() {
    const state = window.history.state;
    const queryEmail = this.route.snapshot.queryParamMap.get('email');
    const queryCode = this.route.snapshot.queryParamMap.get('code');

    const email = state?.email ?? queryEmail;
    if (email) {
      this.email.set(email);
    } else {
      this.router.navigate(['/signup']);
      return;
    }

    if (queryCode) {
      const digits = queryCode.replace(/\D/g, '').slice(0, 6).split('');
      digits.forEach((d, i) => { this.digits[i] = d; });
      if (this.code.length === 6) {
        // Auto-submit slightly delayed so the user sees the pre-filled code
        setTimeout(() => this.onVerify(), 300);
      }
    }
  }

  ngOnDestroy() {
    if (this.cooldownInterval) {
      clearInterval(this.cooldownInterval);
    }
  }

  get code(): string {
    return this.digits.join('');
  }

  onDigitInput(index: number, event: Event) {
    const input = event.target as HTMLInputElement;
    const val = input.value.replace(/\D/g, '');
    if (val.length > 1) {
      // Handle paste via input event
      const chars = val.slice(0, 6).split('');
      chars.forEach((c, i) => {
        if (index + i < 6) this.digits[index + i] = c;
      });
      const nextIndex = Math.min(index + chars.length, 5);
      this.focusDigit(nextIndex);
    } else {
      this.digits[index] = val;
      if (val && index < 5) {
        this.focusDigit(index + 1);
      }
    }
  }

  onDigitKeydown(index: number, event: KeyboardEvent) {
    if (event.key === 'Backspace' && !this.digits[index] && index > 0) {
      this.focusDigit(index - 1);
    }
  }

  onDigitPaste(index: number, event: ClipboardEvent) {
    event.preventDefault();
    const text = event.clipboardData?.getData('text') ?? '';
    const digits = text.replace(/\D/g, '').slice(0, 6).split('');
    digits.forEach((d, i) => {
      if (index + i < 6) this.digits[index + i] = d;
    });
    const nextIndex = Math.min(index + digits.length, 5);
    this.focusDigit(nextIndex);
  }

  private focusDigit(index: number) {
    setTimeout(() => {
      const el = document.getElementById(`digit-${index}`);
      if (el) (el as HTMLInputElement).focus();
    }, 0);
  }

  onVerify() {
    this.error.set('');
    this.loading.set(true);
    this.auth.verifyEmail(this.email(), this.code).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/login'], { state: { verified: true } });
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err?.error?.message ?? this.translate.instant('VERIFY_EMAIL.INVALID_CODE');
        this.error.set(typeof msg === 'string' ? msg : this.translate.instant('VERIFY_EMAIL.INVALID_CODE'));
      }
    });
  }

  onResend() {
    this.error.set('');
    this.resendLoading.set(true);
    this.auth.resendVerification(this.email()).subscribe({
      next: () => {
        this.resendLoading.set(false);
        this.startCooldown();
      },
      error: (err) => {
        this.resendLoading.set(false);
        const msg = err?.error?.message ?? this.translate.instant('VERIFY_EMAIL.SEND_ERROR');
        this.error.set(typeof msg === 'string' ? msg : this.translate.instant('VERIFY_EMAIL.SEND_ERROR'));
      }
    });
  }

  private startCooldown() {
    this.resendCooldown.set(60);
    this.cooldownInterval = setInterval(() => {
      const current = this.resendCooldown();
      if (current <= 1) {
        this.resendCooldown.set(0);
        if (this.cooldownInterval) {
          clearInterval(this.cooldownInterval);
          this.cooldownInterval = null;
        }
      } else {
        this.resendCooldown.set(current - 1);
      }
    }, 1000);
  }
}

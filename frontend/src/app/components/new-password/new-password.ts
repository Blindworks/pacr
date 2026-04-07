import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-new-password',
  standalone: true,
  imports: [RouterLink, FormsModule, TranslateModule],
  templateUrl: './new-password.html',
  styleUrl: './new-password.scss'
})
export class NewPassword implements OnInit {
  password = signal('');
  confirmPassword = signal('');
  showPassword = signal(false);
  showConfirmPassword = signal(false);
  loading = signal(false);
  errorKey = signal<string | null>(null);
  successKey = signal<string | null>(null);

  hasMinLength = computed(() => this.password().length >= 10);
  hasUpper = computed(() => /[A-Z]/.test(this.password()));
  hasLower = computed(() => /[a-z]/.test(this.password()));
  hasNumber = computed(() => /\d/.test(this.password()));
  hasSpecial = computed(() => /[^A-Za-z0-9]/.test(this.password()));
  allRulesMet = computed(() =>
    this.hasMinLength() && this.hasUpper() && this.hasLower() && this.hasNumber() && this.hasSpecial()
  );
  passwordsMatch = computed(() => this.password() === this.confirmPassword() && this.confirmPassword().length > 0);

  private token = '';
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  constructor(private translate: TranslateService) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.router.navigate(['/forgot-password']);
    }
  }

  onPasswordInput(event: Event) {
    this.password.set((event.target as HTMLInputElement).value);
  }

  onConfirmInput(event: Event) {
    this.confirmPassword.set((event.target as HTMLInputElement).value);
  }

  onSubmit() {
    if (!this.allRulesMet() || !this.passwordsMatch()) return;
    if (!this.token || this.loading()) return;
    this.loading.set(true);
    this.errorKey.set(null);
    this.auth.resetPassword(this.token, this.password()).subscribe({
      next: () => {
        this.loading.set(false);
        this.successKey.set('NEW_PASSWORD.SUCCESS');
        setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: () => {
        this.loading.set(false);
        this.errorKey.set('NEW_PASSWORD.INVALID_OR_EXPIRED');
      }
    });
  }
}

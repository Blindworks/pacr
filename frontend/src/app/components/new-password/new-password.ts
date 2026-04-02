import { Component, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-new-password',
  standalone: true,
  imports: [RouterLink, FormsModule, TranslateModule],
  templateUrl: './new-password.html',
  styleUrl: './new-password.scss'
})
export class NewPassword {
  password = signal('');
  confirmPassword = signal('');
  showPassword = signal(false);
  showConfirmPassword = signal(false);

  hasMinLength = computed(() => this.password().length >= 8);
  hasNumber = computed(() => /\d/.test(this.password()));
  hasSpecial = computed(() => /[^a-zA-Z0-9]/.test(this.password()));
  passwordsMatch = computed(() => this.password() === this.confirmPassword() && this.confirmPassword().length > 0);

  constructor(private translate: TranslateService) {}

  onPasswordInput(event: Event) {
    this.password.set((event.target as HTMLInputElement).value);
  }

  onConfirmInput(event: Event) {
    this.confirmPassword.set((event.target as HTMLInputElement).value);
  }

  onSubmit() {
    if (!this.hasMinLength() || !this.hasNumber() || !this.hasSpecial() || !this.passwordsMatch()) return;
    // TODO: call reset password API
  }
}

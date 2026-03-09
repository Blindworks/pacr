import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-forgot-password-confirmation',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './forgot-password-confirmation.html',
  styleUrl: './forgot-password-confirmation.scss'
})
export class ForgotPasswordConfirmation {
  resent = false;

  onResend() {
    this.resent = true;
  }
}

import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-forgot-password-confirmation',
  standalone: true,
  imports: [RouterLink, TranslateModule],
  templateUrl: './forgot-password-confirmation.html',
  styleUrl: './forgot-password-confirmation.scss'
})
export class ForgotPasswordConfirmation {
  resent = false;

  constructor(private translate: TranslateService) {}

  onResend() {
    this.resent = true;
  }
}

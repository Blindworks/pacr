import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-forgot-password-confirmation',
  standalone: true,
  imports: [RouterLink, TranslateModule],
  templateUrl: './forgot-password-confirmation.html',
  styleUrl: './forgot-password-confirmation.scss'
})
export class ForgotPasswordConfirmation {
  resent = false;
  private readonly email: string;
  private readonly auth = inject(AuthService);

  constructor(private translate: TranslateService, route: ActivatedRoute) {
    this.email = route.snapshot.queryParamMap.get('email') ?? '';
  }

  onResend() {
    if (!this.email) {
      this.resent = true;
      return;
    }
    const done = () => { this.resent = true; };
    this.auth.forgotPassword(this.email).subscribe({ next: done, error: done });
  }
}

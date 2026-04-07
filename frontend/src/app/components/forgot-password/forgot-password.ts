import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, RouterLink, TranslateModule],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.scss'
})
export class ForgotPassword {
  email = '';
  loading = false;

  private readonly auth = inject(AuthService);

  constructor(private router: Router, private translate: TranslateService) {}

  onSubmit() {
    if (!this.email || this.loading) return;
    this.loading = true;
    const email = this.email;
    const navigate = () => {
      this.loading = false;
      this.router.navigate(['/forgot-password/confirmation'], { queryParams: { email } });
    };
    this.auth.forgotPassword(email).subscribe({ next: navigate, error: navigate });
  }
}

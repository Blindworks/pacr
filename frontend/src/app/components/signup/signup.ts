import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './signup.html',
  styleUrl: './signup.scss'
})
export class Signup {
  fullName = '';
  username = '';
  email = '';
  password = '';
  showPassword = false;

  constructor(private router: Router) {}

  onSubmit() {
    this.router.navigate(['/']);
  }
}

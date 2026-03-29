import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { NgClass } from '@angular/common';
import { PaceCalculatorService } from '../../services/pace-calculator.service';
import { AboutDialogService } from '../../services/about-dialog.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgClass],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class Sidebar implements OnInit, OnDestroy {
  bodyDataOpen = false;
  profileImageUrl = signal<string | null>(null);

  private readonly paceCalc = inject(PaceCalculatorService);
  private readonly aboutDialog = inject(AboutDialogService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly userService = inject(UserService);

  readonly isAdmin = computed(() => {
    const role = this.userService.currentUser()?.role ?? this.authService.getRole();
    return role === 'ADMIN';
  });

  readonly membershipLabel = computed(() => {
    const role = this.userService.currentUser()?.role;
    if (role === 'ADMIN') return 'Admin';
    if (role === 'USER') return 'Member';
    return 'Member';
  });

  ngOnInit(): void {
    if (!this.userService.currentUser()) {
      this.userService.getMe().subscribe({
        next: user => this.loadProfileImage(user.id),
        error: () => {}
      });
    } else {
      const user = this.userService.currentUser();
      if (user) this.loadProfileImage(user.id);
    }
  }

  ngOnDestroy(): void {
    const url = this.profileImageUrl();
    if (url) URL.revokeObjectURL(url);
  }

  private loadProfileImage(userId: number): void {
    this.userService.getProfileImage(userId).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const old = this.profileImageUrl();
        if (old) URL.revokeObjectURL(old);
        this.profileImageUrl.set(url);
      },
      error: () => {}
    });
  }

  toggleBodyData(): void {
    this.bodyDataOpen = !this.bodyDataOpen;
  }

  openPaceCalculator(): void {
    this.paceCalc.open();
  }

  openAbout(): void {
    this.aboutDialog.open();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}

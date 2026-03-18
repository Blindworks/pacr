import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgClass } from '@angular/common';
import { PaceCalculatorService } from '../../services/pace-calculator.service';
import { UserService } from '../../services/user.service';

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
  protected readonly userService = inject(UserService);

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
}

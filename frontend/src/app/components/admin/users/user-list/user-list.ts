import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { SlicePipe } from '@angular/common';

import { UserService, UserProfile } from '../../../../services/user.service';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [SlicePipe],
  templateUrl: './user-list.html',
  styleUrl: './user-list.scss'
})
export class UserList implements OnInit {
  private userService = inject(UserService);
  private router = inject(Router);

  users = signal<UserProfile[]>([]);
  isLoading = signal(false);
  hasError = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.hasError.set(false);
    this.userService.getAllUsers().subscribe({
      next: (data) => this.users.set(data),
      error: () => { this.hasError.set(true); this.isLoading.set(false); },
      complete: () => this.isLoading.set(false)
    });
  }

  navigateEdit(id: number): void {
    this.router.navigate(['/admin/users', id, 'edit']);
  }

  statusLabel(status: string | null): string {
    switch (status) {
      case 'ACTIVE': return 'Active';
      case 'BLOCKED': return 'Blocked';
      case 'PENDING': return 'Pending';
      case 'INACTIVE': return 'Inactive';
      default: return status ?? '—';
    }
  }

  statusClass(status: string | null): string {
    switch (status) {
      case 'ACTIVE': return 'status-active';
      case 'BLOCKED': return 'status-blocked';
      case 'PENDING': return 'status-pending';
      case 'INACTIVE': return 'status-inactive';
      default: return '';
    }
  }

  roleLabel(role: string | null): string {
    switch (role) {
      case 'USER': return 'User';
      case 'TRAINER': return 'Trainer';
      case 'ADMIN': return 'Admin';
      default: return role ?? '—';
    }
  }

  fullName(user: UserProfile): string {
    const parts = [user.firstName, user.lastName].filter(Boolean);
    return parts.length > 0 ? parts.join(' ') : '—';
  }

  subscriptionLabel(plan: string | null): string {
    switch (plan) {
      case 'PRO': return 'Pro';
      case 'FREE': return 'Free';
      default: return plan ?? 'Free';
    }
  }

  subscriptionClass(plan: string | null): string {
    return plan === 'PRO' ? 'sub-pro' : 'sub-free';
  }

  formatDate(dt: string | null): string {
    if (!dt) return '—';
    return new Date(dt).toLocaleString('de-DE', { dateStyle: 'short', timeStyle: 'short' });
  }
}

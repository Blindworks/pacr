import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { SlicePipe } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { UserService, UserProfile } from '../../../../services/user.service';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [SlicePipe, TranslateModule],
  templateUrl: './user-list.html',
  styleUrl: './user-list.scss'
})
export class UserList implements OnInit {
  private userService = inject(UserService);
  private router = inject(Router);
  private translate = inject(TranslateService);

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
      case 'ACTIVE': return this.translate.instant('ADMIN.STATUS_ACTIVE');
      case 'BLOCKED': return this.translate.instant('ADMIN.STATUS_BLOCKED');
      case 'PENDING': return this.translate.instant('ADMIN.STATUS_PENDING');
      case 'INACTIVE': return this.translate.instant('ADMIN.STATUS_INACTIVE');
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
      case 'USER': return this.translate.instant('ADMIN.ROLE_USER');
      case 'TRAINER': return this.translate.instant('ADMIN.ROLE_TRAINER');
      case 'ADMIN': return this.translate.instant('ADMIN.ROLE_ADMIN');
      default: return role ?? '—';
    }
  }

  fullName(user: UserProfile): string {
    const parts = [user.firstName, user.lastName].filter(Boolean);
    return parts.length > 0 ? parts.join(' ') : '—';
  }

  subscriptionLabel(plan: string | null): string {
    switch (plan) {
      case 'PRO': return this.translate.instant('ADMIN.SUB_PRO');
      case 'FREE': return this.translate.instant('ADMIN.SUB_FREE');
      default: return plan ?? this.translate.instant('ADMIN.SUB_FREE');
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

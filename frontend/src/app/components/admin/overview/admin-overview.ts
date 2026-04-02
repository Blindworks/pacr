import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AdminService, AdminStats, AuditLogEntry } from '../../../services/admin.service';

@Component({
  selector: 'app-admin-overview',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, TranslateModule],
  templateUrl: './admin-overview.html',
  styleUrl: './admin-overview.scss'
})
export class AdminOverview implements OnInit {
  private adminService = inject(AdminService);
  private translate = inject(TranslateService);

  stats = signal<AdminStats | null>(null);
  auditLog = signal<AuditLogEntry[]>([]);
  selectedEntry = signal<AuditLogEntry | null>(null);
  totalElements = signal(0);
  totalPages = signal(0);
  isLoadingStats = signal(false);
  isLoadingLog = signal(false);
  hasError = signal(false);

  // Filter
  filterAction = '';
  filterFrom = '';
  filterTo = '';
  currentPage = 0;
  pageSize = 50;

  get actionCategories(): { label: string; value: string }[] {
    return [
      { label: this.translate.instant('ADMIN.AUDIT_ALL'), value: '' },
      { label: this.translate.instant('ADMIN.AUDIT_LOGIN'), value: 'LOGIN' },
      { label: this.translate.instant('ADMIN.AUDIT_USER_CREATED'), value: 'USER_CREATED' },
      { label: this.translate.instant('ADMIN.AUDIT_USER_EDITED'), value: 'USER_UPDATED' },
      { label: this.translate.instant('ADMIN.AUDIT_STATUS_CHANGED'), value: 'USER_STATUS_CHANGED' },
      { label: this.translate.instant('ADMIN.AUDIT_SUB_CHANGED'), value: 'SUBSCRIPTION_CHANGED' },
      { label: this.translate.instant('ADMIN.AUDIT_FIT_UPLOAD'), value: 'FIT_UPLOADED' },
      { label: this.translate.instant('ADMIN.AUDIT_STRAVA_CONNECTED'), value: 'STRAVA_CONNECTED' },
      { label: this.translate.instant('ADMIN.AUDIT_STRAVA_DISCONNECTED'), value: 'STRAVA_DISCONNECTED' },
      { label: this.translate.instant('ADMIN.AUDIT_PLAN_CREATED'), value: 'PLAN_CREATED' },
      { label: this.translate.instant('ADMIN.AUDIT_PLAN_DELETED'), value: 'PLAN_DELETED' },
      { label: this.translate.instant('ADMIN.AUDIT_COMP_CREATED'), value: 'COMPETITION_CREATED' },
      { label: this.translate.instant('ADMIN.AUDIT_COMP_DELETED'), value: 'COMPETITION_DELETED' },
    ];
  }

  ngOnInit(): void {
    this.loadStats();
    this.loadAuditLog();
  }

  loadStats(): void {
    this.isLoadingStats.set(true);
    this.adminService.getStats().subscribe({
      next: (data) => { this.stats.set(data); this.isLoadingStats.set(false); },
      error: () => { this.hasError.set(true); this.isLoadingStats.set(false); }
    });
  }

  loadAuditLog(): void {
    this.isLoadingLog.set(true);
    this.adminService.getAuditLog({
      page: this.currentPage,
      size: this.pageSize,
      action: this.filterAction || undefined,
      from: this.filterFrom || undefined,
      to: this.filterTo || undefined
    }).subscribe({
      next: (data) => {
        this.auditLog.set(data.content);
        this.totalElements.set(data.totalElements);
        this.totalPages.set(data.totalPages);
        this.isLoadingLog.set(false);
      },
      error: () => { this.hasError.set(true); this.isLoadingLog.set(false); }
    });
  }

  applyFilter(): void {
    this.currentPage = 0;
    this.loadAuditLog();
  }

  goToPage(page: number): void {
    this.currentPage = page;
    this.loadAuditLog();
  }

  formatDetails(details: string | null): string {
    if (!details) return '—';
    try {
      return JSON.stringify(JSON.parse(details), null, 2);
    } catch {
      return details;
    }
  }

  formatDetailsShort(details: string | null): string {
    if (!details) return '—';
    try {
      const obj = JSON.parse(details);
      const parts = Object.entries(obj).map(([k, v]) => `${k}=${v}`);
      const joined = parts.join(', ');
      return joined.length > 60 ? joined.substring(0, 57) + '...' : joined;
    } catch {
      return details.length > 60 ? details.substring(0, 57) + '...' : details;
    }
  }

  openDetailsDialog(entry: AuditLogEntry): void {
    this.selectedEntry.set(entry);
  }

  closeDetailsDialog(): void {
    this.selectedEntry.set(null);
  }

  getDetailsKeyValues(details: string | null): { key: string; value: string }[] {
    if (!details) return [];
    try {
      const obj = JSON.parse(details);
      return Object.entries(obj).map(([key, value]) => ({
        key,
        value: String(value)
      }));
    } catch {
      return [{ key: 'raw', value: details }];
    }
  }
}

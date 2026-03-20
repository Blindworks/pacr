import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, AdminStats, AuditLogEntry } from '../../../services/admin.service';

@Component({
  selector: 'app-admin-overview',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './admin-overview.html',
  styleUrl: './admin-overview.scss'
})
export class AdminOverview implements OnInit {
  private adminService = inject(AdminService);

  stats = signal<AdminStats | null>(null);
  auditLog = signal<AuditLogEntry[]>([]);
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

  readonly actionCategories = [
    { label: 'Alle', value: '' },
    { label: 'Login', value: 'LOGIN' },
    { label: 'User erstellt', value: 'USER_CREATED' },
    { label: 'User bearbeitet', value: 'USER_UPDATED' },
    { label: 'Status geändert', value: 'USER_STATUS_CHANGED' },
    { label: 'Subscription geändert', value: 'SUBSCRIPTION_CHANGED' },
    { label: 'FIT Upload', value: 'FIT_UPLOADED' },
    { label: 'Strava verbunden', value: 'STRAVA_CONNECTED' },
    { label: 'Strava getrennt', value: 'STRAVA_DISCONNECTED' },
    { label: 'Plan erstellt', value: 'PLAN_CREATED' },
    { label: 'Plan gelöscht', value: 'PLAN_DELETED' },
    { label: 'Competition erstellt', value: 'COMPETITION_CREATED' },
    { label: 'Competition gelöscht', value: 'COMPETITION_DELETED' },
  ];

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
}

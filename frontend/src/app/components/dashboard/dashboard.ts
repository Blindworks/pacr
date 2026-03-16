import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService, DashboardData } from '../../services/dashboard.service';

const DAYS_DE = ['So', 'Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa'];

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
  private readonly dashboardService = inject(DashboardService);

  data: DashboardData | null = null;

  ngOnInit(): void {
    this.dashboardService.getDashboard().subscribe({
      next: data => { this.data = data; },
      error: err => console.error('Dashboard load failed', err)
    });
  }

  // ── Load Trend Bar Chart ─────────────────────────────────────────
  barPoints(): Array<{ height: number; label: string; active: boolean }> {
    const trend = this.data?.loadTrend ?? [];
    const recent = trend.slice(-7);
    const max = Math.max(...recent.map(p => p.strain21), 0.1);
    const today = new Date().toISOString().slice(0, 10);
    return recent.map(p => ({
      height: Math.max(4, Math.round(p.strain21 / max * 100)),
      label: DAYS_DE[new Date(p.date).getDay()],
      active: p.date === today
    }));
  }

  // ── VO2 Max Gauge ────────────────────────────────────────────────
  gaugeOffset(): number {
    const vo2 = this.data?.vo2max ?? 0;
    const pct = Math.min(1, Math.max(0, (vo2 - 25) / 45));
    return Math.round(502 * (1 - pct));
  }

  gaugeClass(): 'good' | 'warn' | 'bad' {
    const vo2 = this.data?.vo2max ?? 0;
    if (vo2 >= 50) return 'good';
    if (vo2 >= 40) return 'warn';
    return 'bad';
  }

  gaugeSublabel(): string {
    const vo2 = this.data?.vo2max ?? 0;
    if (vo2 >= 60) return 'Exzellent';
    if (vo2 >= 55) return 'Sehr gut';
    if (vo2 >= 50) return 'Gut';
    if (vo2 >= 45) return 'Durchschnitt';
    if (vo2 >= 40) return 'Unterdurchschnitt';
    return 'Niedrig';
  }

  // ── Load Status ──────────────────────────────────────────────────
  loadFlagClass(): string {
    const flag = this.data?.loadStatus?.flag ?? 'BLUE';
    const map: Record<string, string> = { GREEN: 'good', ORANGE: 'warn', RED: 'bad', BLUE: 'info' };
    return map[flag] ?? 'info';
  }

  loadFlagLabel(): string {
    const flag = this.data?.loadStatus?.flag ?? 'BLUE';
    const map: Record<string, string> = {
      GREEN: 'Optimal',
      ORANGE: 'Vorsicht',
      RED: 'Überlastung',
      BLUE: 'Aufbau'
    };
    return map[flag] ?? flag;
  }

  // ── Training Progress ────────────────────────────────────────────
  progressPct(completed: number, total: number): number {
    return total > 0 ? Math.round(completed / total * 100) : 0;
  }

  // ── Last Run ─────────────────────────────────────────────────────
  hasLastRun(): boolean {
    return this.data?.lastRun != null && (this.data.lastRun.strain21 > 0 || (this.data.lastRun.coachBullets?.length ?? 0) > 0);
  }

  formatDate(dateStr: string | null | undefined): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }
}

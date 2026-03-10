import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ActivityService, CompletedTraining } from '../../services/activity.service';
import { StravaService } from '../../services/strava.service';
import { catchError, of, finalize, timeout } from 'rxjs';

interface Activity {
  id: number;
  name: string;
  date: string;
  distance: string;
  time: string;
  pace: string;
  elevGain: string;
  sport: string;
  sportIcon: string;
}

@Component({
  selector: 'app-activities',
  standalone: true,
  imports: [RouterModule, CommonModule],
  templateUrl: './activities.html',
  styleUrl: './activities.scss'
})
export class Activities implements OnInit {
  private readonly activityService = inject(ActivityService);
  private readonly stravaService = inject(StravaService);
  private readonly cdr = inject(ChangeDetectorRef);

  selectedFilter: string = 'all';
  isLoading = true;
  isSyncing = false;
  hasError = false;
  stravaConnected = false;

  filters = [
    { id: 'all', label: 'All' },
    { id: 'run', label: 'Running' },
    { id: 'cycl', label: 'Cycling' },
    { id: 'swim', label: 'Swimming' },
  ];

  activities: Activity[] = [];

  /** Offset in weeks from current week (0 = this week, -1 = last week, ...) */
  weekOffset = 0;

  get isCurrentWeek(): boolean {
    return this.weekOffset === 0;
  }

  private get weekRange(): { start: string; end: string } {
    const today = new Date();
    const day = today.getDay();
    const diffToMonday = (day === 0 ? -6 : 1 - day);
    const monday = new Date(today);
    monday.setDate(today.getDate() + diffToMonday + this.weekOffset * 7);
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    return {
      start: this.toIsoDate(monday),
      end: this.toIsoDate(sunday),
    };
  }

  prevWeek(): void {
    this.weekOffset--;
    this.loadWeek();
  }

  nextWeek(): void {
    if (this.isCurrentWeek) return;
    this.weekOffset++;
    this.loadWeek();
  }

  private toIsoDate(d: Date): string {
    return d.toISOString().slice(0, 10);
  }

  syncDone = false;

  ngOnInit(): void {
    this.stravaService.getStatus().subscribe({
      next: (status) => {
        this.stravaConnected = status.connected;
        this.cdr.detectChanges();
      }
    });
    this.loadWeek();
  }

  manualSync(): void {
    if (this.isSyncing) return;
    const { start, end } = this.weekRange;
    this.isSyncing = true;
    this.syncDone = false;
    this.stravaService.syncActivities(start, end).pipe(
      timeout(60000),
      catchError((err) => {
        console.error('[Activities] sync error:', err);
        return of(null);
      }),
      finalize(() => {
        this.isSyncing = false;
        this.syncDone = true;
        this.cdr.detectChanges();
      })
    ).subscribe(() => {
      this.loadWeek();
    });
  }

  private loadWeek(): void {
    this.isLoading = true;
    this.hasError = false;
    const { start, end } = this.weekRange;

    this.activityService.getByDateRange(start, end).pipe(
      catchError((err) => {
        console.error('[Activities] error loading activities:', err);
        this.hasError = true;
        return of([]);
      }),
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (data) => {
        this.activities = (data as CompletedTraining[]).map(ct => this.mapToActivity(ct));
      }
    });
  }

  get featuredActivity(): Activity | null {
    return this.activities[0] ?? null;
  }

  get listActivities(): Activity[] {
    return this.activities.slice(1);
  }

  get filteredListActivities(): Activity[] {
    if (this.selectedFilter === 'all') return this.listActivities;
    return this.listActivities.filter(a =>
      a.sport.toLowerCase().includes(this.selectedFilter.toLowerCase())
    );
  }

  get filteredFeatured(): Activity | null {
    if (!this.featuredActivity) return null;
    if (this.selectedFilter === 'all') return this.featuredActivity;
    return this.featuredActivity.sport.toLowerCase().includes(this.selectedFilter.toLowerCase())
      ? this.featuredActivity
      : null;
  }

  showFeatured(): boolean {
    return !!this.filteredFeatured;
  }

  setFilter(id: string): void {
    this.selectedFilter = id;
  }

  get weekLabel(): string {
    const { start, end } = this.weekRange;
    const s = new Date(start);
    const e = new Date(end);
    const fmt = (d: Date) => d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    return `${fmt(s)} – ${fmt(e)}, ${e.getFullYear()}`;
  }

  private mapToActivity(ct: CompletedTraining): Activity {
    return {
      id: ct.id,
      name: ct.activityName ?? ct.trainingType ?? this.formatSport(ct.sport) ?? 'Activity',
      date: this.formatDate(ct.trainingDate),
      distance: ct.distanceKm != null ? `${ct.distanceKm.toFixed(1)} km` : '—',
      time: ct.durationSeconds != null ? this.formatDuration(ct.durationSeconds) : '—',
      pace: ct.averagePaceSecondsPerKm != null ? this.formatPace(ct.averagePaceSecondsPerKm) : '—',
      elevGain: ct.elevationGainM != null ? `${ct.elevationGainM} m` : '—',
      sport: ct.sport ?? 'unknown',
      sportIcon: this.getSportIcon(ct.sport),
    };
  }

  private formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
  }

  private formatDuration(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    if (h > 0) {
      return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    }
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  private formatPace(secondsPerKm: number): string {
    const m = Math.floor(secondsPerKm / 60);
    const s = secondsPerKm % 60;
    return `${m}'${s.toString().padStart(2, '0')}" /km`;
  }

  private formatSport(sport: string | null): string {
    if (!sport) return 'Activity';
    return sport.charAt(0).toUpperCase() + sport.slice(1).toLowerCase().replace('_', ' ');
  }

  private getSportIcon(sport: string | null): string {
    const s = (sport ?? '').toLowerCase();
    if (s.includes('run')) return 'directions_run';
    if (s.includes('cycl') || s.includes('bik') || s.includes('ride')) return 'directions_bike';
    if (s.includes('swim')) return 'pool';
    if (s.includes('walk') || s.includes('hik')) return 'hiking';
    return 'fitness_center';
  }
}

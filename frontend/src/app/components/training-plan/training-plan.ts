import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { UserTrainingEntry, UserTrainingEntryService } from '../../services/user-training-entry.service';

interface TrainingSession {
  id: number;
  entryId?: number;
  title: string;
  subtitle: string;
  status: 'completed' | 'today' | 'upcoming' | 'skipped';
  icon?: string;
  competitionName?: string;
}

interface TrainingDay {
  dayShort: string;
  dayNum: number;
  isoDate: string;
  status: 'completed' | 'today' | 'upcoming' | 'rest' | 'skipped' | 'mixed';
  sessions: TrainingSession[];
}

interface Stat {
  label: string;
  value: string;
  unit: string;
}

interface Insight {
  title: string;
  text: string;
}

const DAY_SHORTS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

@Component({
  selector: 'app-training-plan',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './training-plan.html',
  styleUrl: './training-plan.scss'
})
export class TrainingPlan implements OnInit {
  private router = inject(Router);
  private entryService = inject(UserTrainingEntryService);
  private cdr = inject(ChangeDetectorRef);

  planName = '—';
  weekLabel = '—';
  progressPercent = 0;
  completedSessions = 0;
  totalSessions = 0;
  isLoading = true;
  hasError = false;
  hasPlan = false;

  currentWeekLabel = 'Diese Woche';
  weekOffset = 0;

  days: TrainingDay[] = [];

  insights: Insight[] = [
    {
      title: 'Training Load: Optimal',
      text: 'Your recovery scores have been consistent. You\'re ready for today\'s high-intensity session.'
    },
    {
      title: 'Pace Trend',
      text: 'Threshold pace has improved by 4s/km over the last 14 days. Keep it up!'
    }
  ];

  stats: Stat[] = [
    { label: 'Weekly Distance', value: '—', unit: 'km' },
    { label: 'Time on Feet', value: '—', unit: 'h' },
    { label: 'Avg. HR', value: '—', unit: 'bpm' },
    { label: 'Elevation', value: '—', unit: 'm' }
  ];

  recoveryScore = 0;

  ngOnInit(): void {
    this.loadWeek();
  }

  private getWeekRange(offset: number): { monday: Date; sunday: Date } {
    const today = new Date();
    const dow = today.getDay();
    const monday = new Date(today);
    monday.setDate(today.getDate() - (dow === 0 ? 6 : dow - 1) + offset * 7);
    monday.setHours(0, 0, 0, 0);
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    return { monday, sunday };
  }

  private toIso(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  private loadWeek(): void {
    this.isLoading = true;
    this.hasError = false;
    this.hasPlan = false;

    const { monday, sunday } = this.getWeekRange(this.weekOffset);
    this.entryService.getCalendar(this.toIso(monday), this.toIso(sunday)).subscribe({
      next: (entries) => {
        this.buildWeek(entries, monday);

        if (entries.length > 0) {
          this.hasPlan = true;

          const uniquePlanNames = Array.from(new Set(
            entries
              .map(entry => entry.training?.trainingPlanName)
              .filter((name): name is string => !!name)
          ));
          this.planName = uniquePlanNames.length <= 1
            ? (uniquePlanNames[0] ?? 'Trainingsplan')
            : `${uniquePlanNames.length} aktive Pläne`;

          const uniqueWeeks = Array.from(new Set(entries.map(entry => entry.weekNumber))).sort((a, b) => a - b);
          this.weekLabel = uniqueWeeks.length <= 1
            ? `Woche ${uniqueWeeks[0]}`
            : `Planwochen ${uniqueWeeks.join(' / ')}`;

          this.completedSessions = entries.filter(entry => entry.completed).length;
          this.totalSessions = entries.length;
          this.progressPercent = this.totalSessions > 0
            ? Math.round((this.completedSessions / this.totalSessions) * 100)
            : 0;
          return;
        }

        this.resetSummary();
      },
      error: (err) => {
        console.error('Fehler beim Laden der Trainingsplan-Einträge:', err);
        this.hasError = true;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      complete: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private resetSummary(): void {
    this.planName = '—';
    this.weekLabel = '—';
    this.completedSessions = 0;
    this.totalSessions = 0;
    this.progressPercent = 0;
  }

  private buildWeek(entries: UserTrainingEntry[], monday: Date): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const entriesByDate = new Map<string, UserTrainingEntry[]>();
    for (const entry of entries) {
      const dayEntries = entriesByDate.get(entry.trainingDate) ?? [];
      dayEntries.push(entry);
      entriesByDate.set(entry.trainingDate, dayEntries);
    }

    this.days = [];
    for (let i = 0; i < 7; i++) {
      const day = new Date(monday);
      day.setDate(monday.getDate() + i);
      const isoDate = this.toIso(day);
      const jsDay = day.getDay();
      const dayEntries = (entriesByDate.get(isoDate) ?? []).sort((left, right) => {
        const planCompare = (left.training?.trainingPlanName ?? '').localeCompare(right.training?.trainingPlanName ?? '');
        if (planCompare !== 0) {
          return planCompare;
        }
        return (left.training?.name ?? '').localeCompare(right.training?.name ?? '');
      });

      if (dayEntries.length === 0) {
        this.days.push({
          dayShort: DAY_SHORTS[jsDay],
          dayNum: day.getDate(),
          isoDate,
          status: 'rest',
          sessions: []
        });
        continue;
      }

      this.days.push({
        dayShort: DAY_SHORTS[jsDay],
        dayNum: day.getDate(),
        isoDate,
        status: this.resolveDayStatus(day, today, dayEntries),
        sessions: dayEntries.map(entry => this.mapSession(entry, day, today))
      });
    }
  }

  private mapSession(entry: UserTrainingEntry, day: Date, today: Date): TrainingSession {
    return {
      id: entry.training.id,
      entryId: entry.id,
      title: entry.training?.name ?? 'Training',
      subtitle: this.buildSubtitle(entry),
      status: this.resolveSessionStatus(entry, day, today),
      icon: this.typeToIcon(entry.training?.trainingType),
      competitionName: entry.competitionName
    };
  }

  private resolveDayStatus(day: Date, today: Date, entries: UserTrainingEntry[]): TrainingDay['status'] {
    const statuses = entries.map(entry => this.resolveSessionStatus(entry, day, today));
    if (statuses.every(status => status === 'completed')) {
      return 'completed';
    }
    if (statuses.every(status => status === 'skipped')) {
      return 'skipped';
    }
    if (statuses.some(status => status === 'completed' || status === 'skipped')) {
      return 'mixed';
    }
    if (day.getTime() === today.getTime()) {
      return 'today';
    }
    return 'upcoming';
  }

  private resolveSessionStatus(entry: UserTrainingEntry, day: Date, today: Date): TrainingSession['status'] {
    if (entry.completionStatus === 'skipped') {
      return 'skipped';
    }
    if (entry.completed) {
      return 'completed';
    }
    if (day.getTime() === today.getTime()) {
      return 'today';
    }
    return 'upcoming';
  }

  private buildSubtitle(entry: UserTrainingEntry): string {
    const parts: string[] = [];
    if (entry.training.estimatedDistanceMeters) {
      parts.push((entry.training.estimatedDistanceMeters / 1000).toFixed(1) + ' km');
    }
    if (entry.training.durationMinutes) {
      parts.push(entry.training.durationMinutes + ' min');
    }
    if (entry.training.intensityLevel) {
      parts.push(entry.training.intensityLevel);
    }
    return parts.join(' • ') || (entry.training.trainingType ?? '');
  }

  private typeToIcon(type?: string): string {
    const map: Record<string, string> = {
      recovery: 'directions_run',
      endurance: 'directions_run',
      speed: 'speed',
      strength: 'fitness_center',
      race: 'flag',
      swimming: 'pool',
      cycling: 'directions_bike'
    };
    return map[type?.toLowerCase() ?? ''] ?? 'directions_run';
  }

  prevWeek(): void {
    this.weekOffset--;
    this.loadWeek();
  }

  nextWeek(): void {
    this.weekOffset++;
    this.loadWeek();
  }

  markSession(session: TrainingSession, status: 'completed' | 'skipped'): void {
    if (!session.entryId) {
      return;
    }

    const nextStatus = session.status === status ? 'upcoming' : status;
    this.entryService.updateFeedback(session.entryId, {
      completed: nextStatus === 'completed',
      completionStatus: nextStatus === 'upcoming' ? 'pending' : nextStatus
    }).subscribe({
      next: () => this.loadWeek(),
      error: (err) => console.error('Fehler beim Aktualisieren des Trainingsstatus:', err)
    });
  }

  showWorkout(session: TrainingSession): void {
    this.viewDetail(session);
  }

  viewDetail(session: TrainingSession): void {
    this.router.navigate(['/training-plans', session.id]);
  }
}

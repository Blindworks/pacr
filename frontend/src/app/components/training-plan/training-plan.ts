import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { UserTrainingEntryService, UserTrainingEntry } from '../../services/user-training-entry.service';

interface TrainingDay {
  id: number;
  dayShort: string;
  dayNum: number;
  title: string;
  subtitle: string;
  status: 'completed' | 'today' | 'upcoming' | 'rest';
  icon?: string;
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

const DAY_NAMES = ['So', 'Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa'];
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

  private toIso(d: Date): string {
    return d.toISOString().split('T')[0];
  }

  private loadWeek(): void {
    this.isLoading = true;
    this.hasError = false;
    const { monday, sunday } = this.getWeekRange(this.weekOffset);
    this.entryService.getCalendar(this.toIso(monday), this.toIso(sunday)).subscribe({
      next: (entries) => {
        this.buildWeek(entries, monday);
        if (entries.length > 0) {
          this.hasPlan = true;
          this.planName = entries[0].training.trainingPlanName ?? 'Trainingsplan';
          const weekNum = entries[0].weekNumber;
          this.weekLabel = `Woche ${weekNum}`;
          this.completedSessions = entries.filter(e => e.completed).length;
          this.totalSessions = entries.length;
          this.progressPercent = this.totalSessions > 0
            ? Math.round((this.completedSessions / this.totalSessions) * 100)
            : 0;
        }
        this.isLoading = false;
      },
      error: () => {
        this.hasError = true;
        this.isLoading = false;
      }
    });
  }

  private buildWeek(entries: UserTrainingEntry[], monday: Date): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const entryByDate = new Map<string, UserTrainingEntry>();
    entries.forEach(e => entryByDate.set(e.trainingDate, e));

    this.days = [];
    for (let i = 0; i < 7; i++) {
      const day = new Date(monday);
      day.setDate(monday.getDate() + i);
      const iso = this.toIso(day);
      const jsDay = day.getDay();
      const entry = entryByDate.get(iso);

      if (entry) {
        let status: TrainingDay['status'];
        if (entry.completed) {
          status = 'completed';
        } else if (day.getTime() === today.getTime()) {
          status = 'today';
        } else {
          status = 'upcoming';
        }
        this.days.push({
          id: entry.id,
          dayShort: DAY_SHORTS[jsDay],
          dayNum: day.getDate(),
          title: entry.training.name,
          subtitle: this.buildSubtitle(entry),
          status,
          icon: this.typeToIcon(entry.training.trainingType)
        });
      } else {
        this.days.push({
          id: -(i + 1),
          dayShort: DAY_SHORTS[jsDay],
          dayNum: day.getDate(),
          title: 'Rest Day',
          subtitle: 'Erholung & Mobilität',
          status: 'rest',
          icon: 'hotel'
        });
      }
    }
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
      'recovery': 'directions_run',
      'endurance': 'directions_run',
      'speed': 'speed',
      'strength': 'fitness_center',
      'race': 'flag',
      'swimming': 'pool',
      'cycling': 'directions_bike',
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

  startWorkout(): void { /* TODO */ }

  viewDetail(id: number): void {
    this.router.navigate(['/training-plans', id]);
  }
}

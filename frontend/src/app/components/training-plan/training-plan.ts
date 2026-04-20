import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { UserTrainingEntry, UserTrainingEntryService } from '../../services/user-training-entry.service';
import { StatisticsService, TrainingStatsDto } from '../../services/statistics.service';
import { PlanAdjustment, PlanAdjustmentService } from '../../services/plan-adjustment.service';
import { CompetitionService } from '../../services/competition.service';

interface TrainingSession {
  id: number;
  entryId?: number;
  title: string;
  subtitle: string;
  status: 'completed' | 'today' | 'upcoming' | 'skipped';
  icon?: string;
  competitionName?: string;
  originalDate?: string;
  isAiMoved?: boolean;
  isGhost?: boolean;
  movedToDate?: string;
}

interface TrainingDay {
  dayShort: string;
  dayNum: number;
  isoDate: string;
  isToday: boolean;
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

export interface ActivePlan {
  competitionId: number;
  competitionName: string;
  planName: string;
}

const DAY_SHORT_KEYS = [
  'COMMON.DAY_SUN', 'COMMON.DAY_MON', 'COMMON.DAY_TUE',
  'COMMON.DAY_WED', 'COMMON.DAY_THU', 'COMMON.DAY_FRI', 'COMMON.DAY_SAT'
];

@Component({
  selector: 'app-training-plan',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './training-plan.html',
  styleUrl: './training-plan.scss'
})
export class TrainingPlan implements OnInit {
  private router = inject(Router);
  private entryService = inject(UserTrainingEntryService);
  private statsService = inject(StatisticsService);
  private adjustmentService = inject(PlanAdjustmentService);
  private competitionService = inject(CompetitionService);
  private cdr = inject(ChangeDetectorRef);
  private translate = inject(TranslateService);

  planName = '—';
  weekLabel = '—';
  progressPercent = 0;
  completedSessions = 0;
  totalSessions = 0;
  isLoading = true;
  hasError = false;
  hasPlan = false;

  currentWeekLabel = '';
  weekOffset = 0;

  days: TrainingDay[] = [];

  insights: Insight[] = [];

  stats: Stat[] = [];

  recoveryScore = 0;

  pendingAdjustments: PlanAdjustment[] = [];

  activePlans: ActivePlan[] = [];
  planPendingLeave: ActivePlan | null = null;
  leaveInProgress = false;

  ngOnInit(): void {
    this.currentWeekLabel = this.translate.instant('TRAINING_PLAN.THIS_WEEK');
    this.insights = [
      {
        title: this.translate.instant('TRAINING_PLAN.INSIGHT_LOAD_TITLE'),
        text: this.translate.instant('TRAINING_PLAN.INSIGHT_LOAD_TEXT')
      },
      {
        title: this.translate.instant('TRAINING_PLAN.INSIGHT_PACE_TITLE'),
        text: this.translate.instant('TRAINING_PLAN.INSIGHT_PACE_TEXT')
      }
    ];
    this.loadWeek();
    this.loadPendingAdjustments();
    this.loadActivePlans();
  }

  private loadActivePlans(): void {
    this.competitionService.getAll().subscribe({
      next: (competitions) => {
        this.activePlans = competitions
          .filter(c => c.registered && c.trainingPlanId)
          .map(c => ({
            competitionId: c.id,
            competitionName: c.name,
            planName: c.trainingPlanName || c.name
          }));
        this.cdr.detectChanges();
      },
      error: () => {
        this.activePlans = [];
      }
    });
  }

  requestLeavePlan(plan: ActivePlan): void {
    this.planPendingLeave = plan;
  }

  cancelLeavePlan(): void {
    if (this.leaveInProgress) return;
    this.planPendingLeave = null;
  }

  confirmLeavePlan(): void {
    const plan = this.planPendingLeave;
    if (!plan || this.leaveInProgress) return;
    this.leaveInProgress = true;
    this.competitionService.unregister(plan.competitionId).subscribe({
      next: () => {
        this.leaveInProgress = false;
        this.planPendingLeave = null;
        this.loadActivePlans();
        this.loadWeek();
        this.loadPendingAdjustments();
      },
      error: (err) => {
        console.error('Failed to leave training plan:', err);
        this.leaveInProgress = false;
        this.planPendingLeave = null;
        this.cdr.detectChanges();
      }
    });
  }

  private loadPendingAdjustments(): void {
    this.adjustmentService.getPending().subscribe({
      next: (adjustments) => {
        this.pendingAdjustments = adjustments;
        this.cdr.detectChanges();
      },
      error: () => {
        this.pendingAdjustments = [];
      }
    });
  }

  acceptAdjustment(adj: PlanAdjustment): void {
    this.adjustmentService.accept(adj.id).subscribe({
      next: () => {
        this.pendingAdjustments = this.pendingAdjustments.filter(a => a.id !== adj.id);
        this.loadWeek();
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Failed to accept adjustment:', err)
    });
  }

  rejectAdjustment(adj: PlanAdjustment): void {
    this.adjustmentService.reject(adj.id).subscribe({
      next: () => {
        this.pendingAdjustments = this.pendingAdjustments.filter(a => a.id !== adj.id);
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Failed to reject adjustment:', err)
    });
  }

  adjustmentIcon(type: string): string {
    switch (type) {
      case 'RESCHEDULE': return 'event';
      case 'DROP': return 'delete_outline';
      case 'INTENSITY_REDUCE': return 'trending_down';
      default: return 'tune';
    }
  }

  adjustmentLabel(type: string): string {
    switch (type) {
      case 'RESCHEDULE': return this.translate.instant('TRAINING_PLAN.ADJ_RESCHEDULE');
      case 'DROP': return this.translate.instant('TRAINING_PLAN.ADJ_DROP');
      case 'INTENSITY_REDUCE': return this.translate.instant('TRAINING_PLAN.ADJ_INTENSITY_REDUCE');
      default: return type;
    }
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

  private static readonly isoDateFormatter = new Intl.DateTimeFormat('sv-SE');

  private toIso(date: Date): string {
    return TrainingPlan.isoDateFormatter.format(date);
  }

  private loadWeek(): void {
    this.isLoading = true;
    this.hasError = false;
    this.hasPlan = false;

    const { monday, sunday } = this.getWeekRange(this.weekOffset);
    forkJoin({
      entries: this.entryService.getCalendar(this.toIso(monday), this.toIso(sunday)),
      stats:   this.statsService.getStatsForDateRange(this.toIso(monday), this.toIso(sunday))
    }).subscribe({
      next: ({ entries, stats }) => {
        this.buildWeek(entries, monday);
        const completed = entries.filter(e => e.completed).length;
        const skipped   = entries.filter(e => e.completionStatus === 'skipped').length;
        this.populateStats(stats, entries.length, completed, skipped);

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
        console.error('Error loading training plan entries:', err);
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

  private populateStats(dto: TrainingStatsDto, total: number, completed: number, skipped: number): void {
    const distance = dto.totalDistanceKm > 0
      ? dto.totalDistanceKm.toFixed(1) : '—';
    const hours = dto.totalDurationSeconds > 0
      ? (dto.totalDurationSeconds / 3600).toFixed(1) : '—';
    const hr = (dto.avgHeartRate != null && dto.avgHeartRate > 0)
      ? Math.round(dto.avgHeartRate).toString() : '—';
    const elevation = (dto.totalElevationGainM != null && dto.totalElevationGainM > 0)
      ? Math.round(dto.totalElevationGainM).toString() : '—';

    this.stats = [
      { label: this.translate.instant('TRAINING_PLAN.WEEKLY_DISTANCE'), value: distance,                    unit: this.translate.instant('COMMON.KM')    },
      { label: this.translate.instant('TRAINING_PLAN.TIME_ON_FEET'),    value: hours,                       unit: this.translate.instant('COMMON.HOURS') },
      { label: this.translate.instant('TRAINING_PLAN.AVG_HR'),          value: hr,                          unit: this.translate.instant('COMMON.BPM')   },
      { label: this.translate.instant('COMMON.ELEVATION'),              value: elevation,                   unit: this.translate.instant('COMMON.M')     },
      { label: this.translate.instant('TRAINING_PLAN.PLANNED'),         value: total > 0 ? String(total) : '—',     unit: this.translate.instant('COMMON.SESSIONS') },
      { label: this.translate.instant('TRAINING_PLAN.DONE'),            value: total > 0 ? String(completed) : '—', unit: this.translate.instant('COMMON.SESSIONS') },
      { label: this.translate.instant('TRAINING_PLAN.SKIPPED'),         value: total > 0 ? String(skipped) : '—',   unit: this.translate.instant('COMMON.SESSIONS') }
    ];
  }

  private buildWeek(entries: UserTrainingEntry[], monday: Date): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const entriesByDate = new Map<string, UserTrainingEntry[]>();
    for (const entry of entries) {
      const dateKey = this.toIso(new Date(entry.trainingDate));
      const dayEntries = entriesByDate.get(dateKey) ?? [];
      dayEntries.push(entry);
      entriesByDate.set(dateKey, dayEntries);
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

      const isToday = day.getTime() === today.getTime();

      if (dayEntries.length === 0) {
        this.days.push({
          dayShort: this.translate.instant(DAY_SHORT_KEYS[jsDay]),
          dayNum: day.getDate(),
          isoDate,
          isToday,
          status: 'rest',
          sessions: []
        });
        continue;
      }

      this.days.push({
        dayShort: this.translate.instant(DAY_SHORT_KEYS[jsDay]),
        dayNum: day.getDate(),
        isoDate,
        isToday,
        status: this.resolveDayStatus(day, today, dayEntries),
        sessions: dayEntries.map(entry => this.mapSession(entry, day, today))
      });
    }

    // Ghost-Einträge für KI-verschobene Trainings auf dem Original-Datum
    for (const entry of entries) {
      if (!entry.originalTrainingDate) continue;
      const origIso = this.toIso(new Date(entry.originalTrainingDate!));
      const origDay = this.days.find(d => d.isoDate === origIso);
      if (!origDay) continue;
      const ghostSession: TrainingSession = {
        id: entry.training.id,
        entryId: entry.id,
        title: entry.training?.name ?? 'Training',
        subtitle: 'Verschoben auf ' + this.formatShortDate(entry.trainingDate),
        status: 'upcoming',
        icon: this.typeToIcon(entry.training?.trainingType),
        competitionName: entry.competitionName,
        isGhost: true,
        movedToDate: entry.trainingDate
      };
      origDay.sessions.push(ghostSession);
    }
  }

  formatShortDate(isoDate: string): string {
    const d = new Date(isoDate);
    const days = ['So', 'Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa'];
    return `${days[d.getDay()]}, ${d.getDate()}.${d.getMonth() + 1}.`;
  }

  private mapSession(entry: UserTrainingEntry, day: Date, today: Date): TrainingSession {
    return {
      id: entry.training.id,
      entryId: entry.id,
      title: entry.training?.name ?? 'Training',
      subtitle: this.buildSubtitle(entry),
      status: this.resolveSessionStatus(entry, day, today),
      icon: this.typeToIcon(entry.training?.trainingType),
      competitionName: entry.competitionName,
      isAiMoved: !!entry.originalTrainingDate,
      originalDate: entry.originalTrainingDate ?? undefined
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
      error: (err) => console.error('Error updating training status:', err)
    });
  }

  showWorkout(session: TrainingSession): void {
    this.viewDetail(session);
  }

  viewDetail(session: TrainingSession): void {
    this.router.navigate(['/training-plans', session.id]);
  }
}

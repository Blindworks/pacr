import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ProOverlay } from '../shared/pro-overlay/pro-overlay';
import {
  DailyCoachService,
  DailyCoachContextDto,
  RecommendationResponse,
  ExecuteResponse
} from '../../services/daily-coach.service';

@Component({
  selector: 'app-ai-trainer',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, ProOverlay, TranslateModule],
  templateUrl: './ai-trainer.component.html',
  styleUrls: ['./ai-trainer.component.scss']
})
export class AiTrainerComponent implements OnInit {
  private coachService = inject(DailyCoachService);
  private readonly translate = inject(TranslateService);

  phase: 'context' | 'recommendation' | 'confirmed' = 'context';
  selectedDate: Date = new Date();
  context: DailyCoachContextDto | null = null;
  feelingScore: number = 3;
  feelingText: string = '';
  recommendation: RecommendationResponse | null = null;
  executing: boolean = false;
  result: ExecuteResponse | null = null;
  loading: boolean = false;
  error: string | null = null;
  aiDisabled: boolean = false;

  readonly feelingEmojis = ['sentiment_very_dissatisfied', 'sentiment_dissatisfied', 'sentiment_neutral', 'sentiment_satisfied', 'sentiment_very_satisfied'];

  get feelingLabels(): string[] {
    return [
      this.translate.instant('AI_TRAINER.FEELING_VERY_BAD'),
      this.translate.instant('AI_TRAINER.FEELING_BAD'),
      this.translate.instant('AI_TRAINER.FEELING_OK'),
      this.translate.instant('AI_TRAINER.FEELING_GOOD'),
      this.translate.instant('AI_TRAINER.FEELING_VERY_GOOD'),
    ];
  }

  ngOnInit(): void {
    this.loadContext();
  }

  get formattedDate(): string {
    return new Intl.DateTimeFormat('sv-SE').format(this.selectedDate);
  }

  get isToday(): boolean {
    const today = new Date();
    return this.selectedDate.toDateString() === today.toDateString();
  }

  get formattedDateDisplay(): string {
    const dayKeys = [
      'DAYS_LONG.SUNDAY', 'DAYS_LONG.MONDAY', 'DAYS_LONG.TUESDAY',
      'DAYS_LONG.WEDNESDAY', 'DAYS_LONG.THURSDAY', 'DAYS_LONG.FRIDAY', 'DAYS_LONG.SATURDAY'
    ];
    const monthKeys = [
      'MONTHS_LONG.JANUARY', 'MONTHS_LONG.FEBRUARY', 'MONTHS_LONG.MARCH',
      'MONTHS_LONG.APRIL', 'MONTHS_LONG.MAY', 'MONTHS_LONG.JUNE',
      'MONTHS_LONG.JULY', 'MONTHS_LONG.AUGUST', 'MONTHS_LONG.SEPTEMBER',
      'MONTHS_LONG.OCTOBER', 'MONTHS_LONG.NOVEMBER', 'MONTHS_LONG.DECEMBER'
    ];
    const d = this.selectedDate;
    const dayName = this.translate.instant(dayKeys[d.getDay()]).slice(0, 2);
    const monthName = this.translate.instant(monthKeys[d.getMonth()]);
    return `${dayName}, ${d.getDate()}. ${monthName} ${d.getFullYear()}`;
  }

  changeDay(delta: number): void {
    const d = new Date(this.selectedDate);
    d.setDate(d.getDate() + delta);
    this.selectedDate = d;
    this.phase = 'context';
    this.context = null;
    this.recommendation = null;
    this.result = null;
    this.error = null;
    this.loadContext();
  }

  loadContext(): void {
    this.loading = true;
    this.error = null;
    this.coachService.getContext(this.formattedDate).subscribe({
      next: (ctx) => {
        this.context = ctx;
        if (ctx.existingSession?.userDecision) {
          this.phase = 'confirmed';
        }
        this.loading = false;
      },
      error: (err) => {
        if (err.status === 503) {
          this.aiDisabled = true;
        } else {
          this.error = this.translate.instant('AI_TRAINER.ERROR_CONTEXT');
        }
        this.loading = false;
      }
    });
  }

  selectFeeling(score: number): void {
    this.feelingScore = score;
  }

  askAI(): void {
    if (!this.context) return;
    this.loading = true;
    this.error = null;
    this.coachService.getRecommendation({
      date: this.formattedDate,
      feelingScore: this.feelingScore,
      feelingText: this.feelingText
    }).subscribe({
      next: (rec) => {
        this.recommendation = rec;
        this.phase = 'recommendation';
        this.loading = false;
      },
      error: () => {
        this.error = this.translate.instant('AI_TRAINER.ERROR_RECOMMENDATION');
        this.loading = false;
      }
    });
  }

  decide(decision: string): void {
    if (!this.recommendation) return;
    this.executing = true;
    this.error = null;
    this.coachService.executeDecision({
      sessionId: this.recommendation.sessionId,
      decision: decision
    }).subscribe({
      next: (res) => {
        this.result = res;
        this.phase = 'confirmed';
        this.executing = false;
      },
      error: () => {
        this.error = this.translate.instant('AI_TRAINER.ERROR_EXECUTION');
        this.executing = false;
      }
    });
  }

  getAsthmaLevelClass(): string {
    const risk = this.context?.asthmaRisk?.riskIndex ?? 0;
    if (risk < 30) return 'risk-low';
    if (risk < 60) return 'risk-medium';
    return 'risk-high';
  }

  getAsthmaLevelText(): string {
    const risk = this.context?.asthmaRisk?.riskIndex ?? 0;
    if (risk < 30) return this.translate.instant('AI_TRAINER.RISK_LOW');
    if (risk < 60) return this.translate.instant('AI_TRAINER.RISK_MEDIUM');
    return this.translate.instant('AI_TRAINER.RISK_HIGH');
  }

  getActionIcon(action: string): string {
    const map: Record<string, string> = {
      PROCEED: 'check_circle',
      MODIFY: 'tune',
      SKIP: 'cancel',
      MOVED: 'event',
      SKIPPED: 'block',
      UNCHANGED: 'check'
    };
    return map[action] ?? 'info';
  }

  getActionLabel(action: string): string {
    const map: Record<string, string> = {
      MOVED: this.translate.instant('AI_TRAINER.ACTION_MOVED'),
      SKIPPED: this.translate.instant('AI_TRAINER.ACTION_SKIPPED'),
      UNCHANGED: this.translate.instant('AI_TRAINER.ACTION_UNCHANGED')
    };
    return map[action] ?? action;
  }

  trackById(_index: number, entry: { id: number }): number {
    return entry.id;
  }

  trackByEntryId(_index: number, change: { entryId: number }): number {
    return change.entryId;
  }
}

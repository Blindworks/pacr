import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AdminAchievementService } from '../../../../services/admin-achievement.service';

@Component({
  selector: 'app-achievement-form',
  standalone: true,
  imports: [FormsModule, TranslateModule],
  templateUrl: './achievement-form.html',
  styleUrl: './achievement-form.scss'
})
export class AchievementForm implements OnInit {
  private achievementService = inject(AdminAchievementService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private translate = inject(TranslateService);

  editId = signal<number | null>(null);
  saving = signal(false);
  error = signal('');

  key = signal('');
  name = signal('');
  description = signal('');
  icon = signal('emoji_events');
  metric = signal('TOTAL_DISTANCE_KM');
  threshold = signal(0);
  sortOrder = signal(0);
  validFrom = signal('');
  validUntil = signal('');

  get metrics() {
    return [
      { value: 'TOTAL_DISTANCE_KM', label: this.translate.instant('ADMIN.METRIC_TOTAL_DISTANCE_KM'), unit: 'km' },
      { value: 'STREAK_DAYS', label: this.translate.instant('ADMIN.METRIC_STREAK_DAYS'), unit: this.translate.instant('ADMIN.UNIT_DAYS') },
      { value: 'PR_TOTAL_COUNT', label: this.translate.instant('ADMIN.METRIC_PR_TOTAL_COUNT'), unit: this.translate.instant('ADMIN.UNIT_PRS') },
      { value: 'PR_DISTINCT_DISTANCES', label: this.translate.instant('ADMIN.METRIC_PR_DISTINCT_DISTANCES'), unit: this.translate.instant('ADMIN.UNIT_DISTANCES') },
      { value: 'PERFECT_WEEKS_COUNT', label: this.translate.instant('ADMIN.METRIC_PERFECT_WEEKS_COUNT'), unit: this.translate.instant('ADMIN.UNIT_WEEKS') },
      { value: 'COMPLETED_PLANS_COUNT', label: this.translate.instant('ADMIN.METRIC_COMPLETED_PLANS_COUNT'), unit: this.translate.instant('ADMIN.UNIT_PLANS') }
    ];
  }

  get currentUnit(): string {
    return this.metrics.find(m => m.value === this.metric())?.unit ?? '';
  }

  readonly icons = [
    'emoji_events', 'directions_run', 'local_fire_department', 'task_alt',
    'military_tech', 'star', 'bolt', 'timer', 'trending_up', 'fitness_center',
    'hiking', 'pool', 'directions_bike', 'speed', 'workspace_premium'
  ];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId.set(+id);
      this.achievementService.getById(+id).subscribe({
        next: a => {
          this.key.set(a.key);
          this.name.set(a.name);
          this.description.set(a.description);
          this.icon.set(a.icon);
          this.metric.set(a.metric ?? 'TOTAL_DISTANCE_KM');
          this.threshold.set(a.threshold);
          this.sortOrder.set(a.sortOrder);
          this.validFrom.set(a.validFrom ?? '');
          this.validUntil.set(a.validUntil ?? '');
        }
      });
    }
  }

  save(): void {
    if (!this.key() || !this.name()) {
      this.error.set(this.translate.instant('ADMIN.KEY_NAME_REQUIRED'));
      return;
    }

    this.saving.set(true);
    this.error.set('');

    const data = {
      key: this.key(),
      name: this.name(),
      description: this.description(),
      icon: this.icon(),
      metric: this.metric(),
      threshold: this.threshold(),
      sortOrder: this.sortOrder(),
      validFrom: this.validFrom() || null,
      validUntil: this.validUntil() || null
    };

    const id = this.editId();
    const call = id
      ? this.achievementService.update(id, data)
      : this.achievementService.create(data);

    call.subscribe({
      next: () => this.router.navigate(['/admin/achievements']),
      error: () => {
        this.error.set(this.translate.instant('ADMIN.ACHIEVEMENT_SAVE_ERROR'));
        this.saving.set(false);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/achievements']);
  }
}

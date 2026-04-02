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
  category = signal('DISTANCE');
  threshold = signal(0);
  sortOrder = signal(0);
  validFrom = signal('');
  validUntil = signal('');

  get categories() {
    return [
      { value: 'DISTANCE', label: this.translate.instant('ADMIN.CAT_DISTANCE_FULL') },
      { value: 'STREAK', label: this.translate.instant('ADMIN.CAT_STREAK_FULL') },
      { value: 'PR', label: this.translate.instant('ADMIN.CAT_PR_FULL') },
      { value: 'PLAN_COMPLETION', label: this.translate.instant('ADMIN.CAT_PLAN_FULL') }
    ];
  }

  readonly icons = [
    'emoji_events', 'directions_run', 'local_fire_department', 'task_alt',
    'military_tech', 'star', 'bolt', 'timer', 'trending_up', 'fitness_center',
    'hiking', 'pool', 'cycling', 'speed', 'workspace_premium'
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
          this.category.set(a.category);
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
      category: this.category(),
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

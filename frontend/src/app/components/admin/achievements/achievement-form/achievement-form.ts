import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AdminAchievementService } from '../../../../services/admin-achievement.service';

@Component({
  selector: 'app-achievement-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './achievement-form.html',
  styleUrl: './achievement-form.scss'
})
export class AchievementForm implements OnInit {
  private achievementService = inject(AdminAchievementService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

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

  readonly categories = [
    { value: 'DISTANCE', label: 'Distanz' },
    { value: 'STREAK', label: 'Streak' },
    { value: 'PR', label: 'Persönlicher Rekord' },
    { value: 'PLAN_COMPLETION', label: 'Plan-Abschluss' }
  ];

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
      this.error.set('Key und Name sind erforderlich.');
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
        this.error.set('Fehler beim Speichern.');
        this.saving.set(false);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/achievements']);
  }
}

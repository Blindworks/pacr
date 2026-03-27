import { Component, OnInit, inject, signal, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { AdminAchievementService, AdminAchievement, UnlockedUser } from '../../../../services/admin-achievement.service';

@Component({
  selector: 'app-achievement-list',
  standalone: true,
  imports: [],
  templateUrl: './achievement-list.html',
  styleUrl: './achievement-list.scss'
})
export class AchievementList implements OnInit {
  private achievementService = inject(AdminAchievementService);
  private router = inject(Router);

  achievements = signal<AdminAchievement[]>([]);
  isLoading = signal(false);
  selectedAchievement = signal<AdminAchievement | null>(null);

  @ViewChild('usersDialog') usersDialogRef!: ElementRef<HTMLDialogElement>;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.achievementService.getAll().subscribe({
      next: data => { this.achievements.set(data); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  goNew(): void {
    this.router.navigate(['/admin/achievements/new']);
  }

  edit(item: AdminAchievement): void {
    this.router.navigate(['/admin/achievements', item.id, 'edit']);
  }

  delete(item: AdminAchievement): void {
    if (!confirm(`Achievement "${item.name}" wirklich löschen?`)) return;
    this.achievementService.delete(item.id).subscribe({
      next: () => this.load()
    });
  }

  showUsers(item: AdminAchievement): void {
    this.achievementService.getById(item.id).subscribe({
      next: detail => {
        this.selectedAchievement.set(detail);
        this.usersDialogRef?.nativeElement?.showModal();
      }
    });
  }

  closeUsersDialog(): void {
    this.usersDialogRef?.nativeElement?.close();
    this.selectedAchievement.set(null);
  }

  onDialogClick(event: MouseEvent): void {
    const dialog = this.usersDialogRef.nativeElement;
    const rect = dialog.getBoundingClientRect();
    if (
      event.clientX < rect.left || event.clientX > rect.right ||
      event.clientY < rect.top || event.clientY > rect.bottom
    ) {
      this.closeUsersDialog();
    }
  }

  categoryLabel(cat: string): string {
    const map: Record<string, string> = {
      DISTANCE: 'Distanz',
      STREAK: 'Streak',
      PR: 'Persönl. Rekord',
      PLAN_COMPLETION: 'Plan'
    };
    return map[cat] ?? cat;
  }

  statusLabel(a: AdminAchievement): string {
    if (!a.timeBound) return 'Permanent';
    if (a.expired) return 'Abgelaufen';
    if (a.active) return 'Aktiv';
    return 'Geplant';
  }

  statusClass(a: AdminAchievement): string {
    if (!a.timeBound) return 'badge--permanent';
    if (a.expired) return 'badge--expired';
    if (a.active) return 'badge--active';
    return 'badge--planned';
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  formatDateTime(dateStr: string | null): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  fullName(user: UnlockedUser): string {
    const parts = [user.firstName, user.lastName].filter(Boolean);
    return parts.length > 0 ? parts.join(' ') : user.username;
  }
}

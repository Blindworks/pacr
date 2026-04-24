import { Component, ElementRef, ViewChild, computed, effect, inject } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { NewStravaActivityDialogService } from '../../services/new-strava-activity-dialog.service';

@Component({
  selector: 'app-new-strava-activity-dialog',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './new-strava-activity-dialog.html',
  styleUrl: './new-strava-activity-dialog.scss'
})
export class NewStravaActivityDialog {
  @ViewChild('dialog') private dialogRef!: ElementRef<HTMLDialogElement>;

  private readonly service = inject(NewStravaActivityDialogService);
  private readonly router = inject(Router);

  protected readonly activity = this.service.activity;

  protected readonly sportIcon = computed(() => {
    const sport = (this.activity()?.sport ?? '').toLowerCase();
    if (sport.includes('run')) return 'directions_run';
    if (sport.includes('ride') || sport.includes('cycl') || sport.includes('bike')) return 'directions_bike';
    if (sport.includes('swim')) return 'pool';
    if (sport.includes('walk') || sport.includes('hike')) return 'directions_walk';
    return 'fitness_center';
  });

  protected readonly distanceLabel = computed(() => {
    const km = this.activity()?.distanceKm;
    return km != null ? `${km.toFixed(2).replace('.', ',')} km` : '—';
  });

  protected readonly durationLabel = computed(() => {
    const sec = this.activity()?.movingTimeSeconds ?? this.activity()?.durationSeconds;
    if (sec == null) return '—';
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    const s = sec % 60;
    return h > 0
      ? `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
      : `${m}:${String(s).padStart(2, '0')}`;
  });

  protected readonly formattedDate = computed(() => {
    const d = this.activity()?.trainingDate;
    if (!d) return '';
    return new Date(d).toLocaleDateString(undefined, { day: '2-digit', month: '2-digit', year: 'numeric' });
  });

  private readonly openEffect = effect(() => {
    const act = this.activity();
    if (!this.dialogRef) return;
    const dialog = this.dialogRef.nativeElement;
    if (act && !dialog.open) {
      dialog.showModal();
    } else if (!act && dialog.open) {
      dialog.close();
    }
  });

  close(): void {
    this.service.close();
  }

  viewDetails(): void {
    const id = this.activity()?.id;
    this.close();
    if (id != null) {
      this.router.navigate(['/activities', id]);
    }
  }

  onBackdropClick(event: MouseEvent): void {
    const rect = this.dialogRef.nativeElement.getBoundingClientRect();
    const outside =
      event.clientX < rect.left || event.clientX > rect.right ||
      event.clientY < rect.top || event.clientY > rect.bottom;
    if (outside) this.close();
  }
}

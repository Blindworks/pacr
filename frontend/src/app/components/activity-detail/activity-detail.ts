import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ActivityService, CompletedTraining } from '../../services/activity.service';

@Component({
  selector: 'app-activity-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './activity-detail.html',
  styleUrl: './activity-detail.scss'
})
export class ActivityDetail implements OnInit {
  activity: CompletedTraining | null = null;
  loading = true;
  error = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private activityService: ActivityService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.activityService.getById(id).subscribe({
      next: (data) => {
        this.activity = data;
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/activities']);
  }

  get formattedDistance(): string {
    return this.activity?.distanceKm != null
      ? this.activity.distanceKm.toFixed(1)
      : '—';
  }

  get formattedDuration(): string {
    const s = this.activity?.durationSeconds;
    if (s == null) return '—';
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    if (h > 0) {
      return `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
    }
    return `${m}:${String(sec).padStart(2, '0')}`;
  }

  get formattedPace(): string {
    const p = this.activity?.averagePaceSecondsPerKm;
    if (p == null) return '—';
    const m = Math.floor(p / 60);
    const s = Math.round(p % 60);
    return `${m}:${String(s).padStart(2, '0')}`;
  }

  get formattedDate(): string {
    const d = this.activity?.trainingDate;
    if (!d) return '—';
    return new Date(d).toLocaleDateString('de-DE', {
      weekday: 'long',
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
  }

  get displayName(): string {
    return this.activity?.activityName || this.activity?.trainingType || this.activity?.sport || 'Training';
  }

  get displayLabel(): string {
    return this.activity?.trainingType || this.activity?.sport || '—';
  }

  get hrPoints(): string {
    const avg = this.activity?.averageHeartRate ?? 150;
    const max = this.activity?.maxHeartRate ?? avg + 20;
    // Generate a simple curve using avg and max
    const points = [
      avg - 10, avg - 5, avg, avg + 5, max, max - 5, avg + 10, avg + 5, avg, avg - 5, avg - 8
    ];
    const w = 1000, h = 256;
    const minBpm = Math.max(100, avg - 30);
    const range = max - minBpm + 10;
    const coords = points.map((bpm, i) => {
      const x = (i / (points.length - 1)) * w;
      const y = h - ((bpm - minBpm) / range) * (h - 20) - 10;
      return `${x} ${y}`;
    });
    return `M ${coords.join(' L ')}`;
  }

  get elevationPath(): string {
    const total = this.activity?.elevationGainM ?? 0;
    const gains = [0, 0.08, 0.25, 0.45, 0.65, 0.8, 0.95, 1.0, 0.9, 0.75, 0.55].map(f => f * total);
    const w = 1000, h = 256;
    const maxGain = Math.max(total, 1);
    const points = gains.map((g, i) => {
      const x = (i / (gains.length - 1)) * w;
      const y = h - (g / maxGain) * (h - 30) - 10;
      return `${x} ${y}`;
    });
    return `M 0 ${h} L ${points.join(' L ')} L ${w} ${h} Z`;
  }

  get midpointHr(): number {
    return this.activity?.averageHeartRate ?? 0;
  }

  get midpointElev(): number {
    return Math.round((this.activity?.elevationGainM ?? 0) * 0.8);
  }
}

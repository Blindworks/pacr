import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ActivityService, ActivityStreamDto, CompletedTraining } from '../../services/activity.service';

@Component({
  selector: 'app-activity-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './activity-detail.html',
  styleUrl: './activity-detail.scss'
})
export class ActivityDetail implements OnInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly activityService = inject(ActivityService);

  activity: CompletedTraining | null = null;
  loading = true;
  error = false;

  streams: ActivityStreamDto | null = null;
  streamsLoading = false;
  streamsError = false;

  hoverIndex: number | null = null;
  hoverPixelX: number | null = null;
  hoverSvgX: number | null = null;
  hoverSvgY: number | null = null;

  private activityId = 0;

  ngOnInit(): void {
    this.activityId = Number(this.route.snapshot.paramMap.get('id'));
    this.activityService.getById(this.activityId).subscribe({
      next: (data) => {
        this.activity = data;
        this.loading = false;
        this.cdr.detectChanges();
        this.activityService.getStreams(this.activityId).subscribe({
          next: (s) => {
            this.streams = s;
            this.cdr.detectChanges();
          },
          error: () => {}
        });
      },
      error: () => {
        this.error = true;
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/activities']);
  }

  fetchStreams(): void {
    this.streamsLoading = true;
    this.streamsError = false;
    this.activityService.fetchStreams(this.activityId).subscribe({
      next: (s) => {
        this.streams = s;
        this.streamsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.streamsError = true;
        this.streamsLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  onChartMouseMove(event: MouseEvent): void {
    if (!this.streams?.hasHeartRate) return;
    const el = event.currentTarget as HTMLElement;
    const ratio = Math.max(0, Math.min(1, event.offsetX / el.offsetWidth));
    const pts = this.streams.distancePoints;
    const maxDist = pts[pts.length - 1];
    const targetDist = ratio * maxDist;

    let nearest = 0;
    let minDiff = Infinity;
    for (let i = 0; i < pts.length; i++) {
      const diff = Math.abs(pts[i] - targetDist);
      if (diff < minDiff) { minDiff = diff; nearest = i; }
    }

    this.hoverIndex = nearest;
    this.hoverPixelX = event.offsetX;
    this.hoverSvgX = ratio * 1000;

    const hr = this.streams.heartRate;
    const validHrs = hr.filter((v): v is number => v != null);
    if (validHrs.length > 0 && hr[nearest] != null) {
      const minHr = Math.min(...validHrs);
      const maxHr = Math.max(...validHrs);
      this.hoverSvgY = 256 - ((hr[nearest]! - minHr) / (maxHr - minHr || 1)) * 216;
    } else {
      this.hoverSvgY = null;
    }
    this.cdr.detectChanges();
  }

  onChartMouseLeave(): void {
    this.hoverIndex = null;
    this.hoverPixelX = null;
    this.hoverSvgX = null;
    this.hoverSvgY = null;
    this.cdr.detectChanges();
  }

  get hoverHr(): number | null {
    if (this.hoverIndex == null || !this.streams?.hasHeartRate) return null;
    return this.streams.heartRate[this.hoverIndex] ?? null;
  }

  get hoverDist(): number | null {
    if (this.hoverIndex == null || !this.streams) return null;
    return this.streams.distancePoints[this.hoverIndex];
  }

  get tooltipLeft(): string {
    return this.hoverPixelX != null ? `${this.hoverPixelX}px` : '50%';
  }

  get isStravaActivity(): boolean {
    return this.activity?.source === 'STRAVA';
  }

  get hasStreams(): boolean {
    return this.streams != null;
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
    if (this.streams?.hasHeartRate) {
      return this.buildSvgPath(
        Array.from(this.streams.distancePoints),
        this.streams.heartRate
      );
    }
    // Fallback: generate a simple curve using avg and max heart rate
    const avg = this.activity?.averageHeartRate ?? 150;
    const max = this.activity?.maxHeartRate ?? avg + 20;
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
    if (this.streams?.hasAltitude) {
      const linePath = this.buildSvgPath(
        Array.from(this.streams.distancePoints),
        this.streams.altitude
      );
      if (linePath) {
        const w = 1000, h = 256;
        return `${linePath} L ${w} ${h} L 0 ${h} Z`;
      }
    }
    // Fallback: generate a simple elevation profile
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

  get formattedBestLap(): string {
    const s = this.activity?.bestLapTimeSeconds;
    if (s == null) return '—';
    const m = Math.floor(s / 60);
    const sec = Math.round(s % 60);
    return `${m}:${String(sec).padStart(2, '0')}`;
  }

  get formattedMaxPace(): string {
    const speed = this.activity?.maxSpeedKmh;
    if (speed == null || speed === 0) return '—';
    const paceSeconds = 3600 / speed;
    const m = Math.floor(paceSeconds / 60);
    const s = Math.round(paceSeconds % 60);
    return `${m}:${String(s).padStart(2, '0')}`;
  }

  get totalZoneSeconds(): number {
    const a = this.activity;
    if (!a) return 0;
    return (a.timeInHrZone1Seconds ?? 0)
      + (a.timeInHrZone2Seconds ?? 0)
      + (a.timeInHrZone3Seconds ?? 0)
      + (a.timeInHrZone4Seconds ?? 0)
      + (a.timeInHrZone5Seconds ?? 0);
  }

  zonePercentage(zone: number): number {
    const total = this.totalZoneSeconds;
    if (total === 0) return 0;
    const a = this.activity!;
    const zoneMap: Record<number, number | null> = {
      1: a.timeInHrZone1Seconds,
      2: a.timeInHrZone2Seconds,
      3: a.timeInHrZone3Seconds,
      4: a.timeInHrZone4Seconds,
      5: a.timeInHrZone5Seconds,
    };
    return ((zoneMap[zone] ?? 0) / total) * 100;
  }

  formattedZoneTime(zone: number): string {
    const a = this.activity;
    if (!a) return '—';
    const zoneMap: Record<number, number | null> = {
      1: a.timeInHrZone1Seconds,
      2: a.timeInHrZone2Seconds,
      3: a.timeInHrZone3Seconds,
      4: a.timeInHrZone4Seconds,
      5: a.timeInHrZone5Seconds,
    };
    const s = zoneMap[zone] ?? 0;
    const m = Math.floor(s / 60);
    const sec = Math.round(s % 60);
    return `${m}:${String(sec).padStart(2, '0')}`;
  }

  get hasHrZones(): boolean {
    const a = this.activity;
    if (!a) return false;
    return (a.timeInHrZone1Seconds ?? 0) > 0
      || (a.timeInHrZone2Seconds ?? 0) > 0
      || (a.timeInHrZone3Seconds ?? 0) > 0
      || (a.timeInHrZone4Seconds ?? 0) > 0
      || (a.timeInHrZone5Seconds ?? 0) > 0;
  }

  get midpointHr(): number {
    return this.activity?.averageHeartRate ?? 0;
  }

  get midpointElev(): number {
    return Math.round((this.activity?.elevationGainM ?? 0) * 0.8);
  }

  get mapVariant(): number {
    return (this.activity?.id ?? 0) % 3;
  }

  private buildSvgPath(xValues: number[], yValues: (number | null)[]): string {
    if (!xValues?.length || !yValues?.length) return '';
    const validPairs = xValues.map((x, i) => ({ x, y: yValues[i] })).filter(p => p.y != null);
    if (validPairs.length < 2) return '';
    const minX = validPairs[0].x;
    const maxX = validPairs[validPairs.length - 1].x;
    const minY = Math.min(...validPairs.map(p => p.y!));
    const maxY = Math.max(...validPairs.map(p => p.y!));
    const scaleX = (x: number) => ((x - minX) / (maxX - minX || 1)) * 1000;
    const scaleY = (y: number) => 256 - ((y - minY) / (maxY - minY || 1)) * 216;
    return validPairs.map((p, i) =>
      `${i === 0 ? 'M' : 'L'}${scaleX(p.x).toFixed(1)},${scaleY(p.y!).toFixed(1)}`
    ).join(' ');
  }
}

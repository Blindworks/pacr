import { Component, OnInit, inject, ChangeDetectorRef, signal } from '@angular/core';
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

  private activityId = 0;

  // Stream registry — defines each stream's display properties
  readonly streamDescriptors = [
    { key: 'heartRate',        hasKey: 'hasHeartRate', label: 'HR',       unit: 'bpm', color: '#ef4444', axis: 'left'  as const, invert: false },
    { key: 'altitude',         hasKey: 'hasAltitude',  label: 'Altitude', unit: 'm',   color: '#60a5fa', axis: 'right' as const, invert: false },
    { key: 'paceSecondsPerKm', hasKey: 'hasPace',      label: 'Pace',     unit: '/km', color: '#4ade80', axis: 'left'  as const, invert: true  },
    { key: 'cadence',          hasKey: 'hasCadence',   label: 'Cadence',  unit: 'spm', color: '#fb923c', axis: 'right' as const, invert: false },
    { key: 'power',            hasKey: 'hasPower',     label: 'Power',    unit: 'W',   color: '#b9f20d', axis: 'left'  as const, invert: false },
  ] as const;

  // Signal — Angular change detection reacts when .set() is called
  activeStreams = signal<Set<string>>(new Set(['heartRate', 'altitude']));

  tooltipIndex: number | null = null;
  tooltipPixelX: number | null = null;

  // Cadence unit: 'rpm' for cycling, 'spm' otherwise
  get cadenceUnit(): string {
    return this.activity?.sport?.toLowerCase().includes('cycling') ||
           this.activity?.sport?.toLowerCase().includes('bike') ? 'rpm' : 'spm';
  }

  toggleStream(key: string): void {
    // Signals require creating a new Set to trigger change detection
    const next = new Set(this.activeStreams());
    if (next.has(key)) { next.delete(key); } else { next.add(key); }
    this.activeStreams.set(next);
  }

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

  onChartMouseLeave(): void {
    this.tooltipIndex = null;
    this.tooltipPixelX = null;
    this.cdr.detectChanges();
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

  /** Returns the data array for a stream key. */
  getStreamData(key: string): (number | null)[] | null {
    if (!this.streams) return null;
    return (this.streams as any)[key] as (number | null)[] | null;
  }

  /** Returns true if the stream has data (via has-flag). */
  streamAvailable(hasKey: string): boolean {
    return this.streams ? !!(this.streams as any)[hasKey] : false;
  }

  /** Returns min/max of a nullable number array, or null if empty. */
  streamRange(data: (number | null)[]): { min: number; max: number } | null {
    const vals = data.filter((v): v is number => v !== null);
    if (vals.length === 0) return null;
    return { min: Math.min(...vals), max: Math.max(...vals) };
  }

  /** Builds SVG path string for one stream. */
  buildStreamPath(
    data: (number | null)[],
    range: { min: number; max: number },
    plotH: number,
    plotW: number,
    invert: boolean
  ): string {
    const dist = this.streams!.distancePoints;
    const maxDist = dist[dist.length - 1] || 1;
    const span = range.max - range.min || 1;
    const parts: string[] = [];
    let penUp = true;
    for (let i = 0; i < data.length; i++) {
      const v = data[i];
      if (v === null || v === undefined) { penUp = true; continue; }
      const x = (dist[i] / maxDist) * plotW;
      const norm = invert ? (range.max - v) / span : (v - range.min) / span;
      const y = plotH - norm * plotH;
      parts.push(`${penUp ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`);
      penUp = false;
    }
    return parts.join(' ');
  }

  /** Returns 5 tick labels for a Y-axis. */
  yAxisTicks(
    range: { min: number; max: number },
    plotH: number,
    invert: boolean,
    isPace: boolean
  ): { y: number; label: string }[] {
    const ticks = [];
    for (let i = 0; i <= 4; i++) {
      const frac = i / 4;
      const val = range.min + frac * (range.max - range.min);
      const y = invert
        ? plotH - ((range.max - val) / (range.max - range.min || 1)) * plotH
        : plotH - frac * plotH;
      ticks.push({ y, label: isPace ? this.formatPace(Math.round(val)) : Math.round(val).toString() });
    }
    return ticks;
  }

  /** Streams that have data AND are toggled on. */
  get availableActiveStreams() {
    const active = this.activeStreams();
    return this.streamDescriptors.filter(d => this.streamAvailable(d.hasKey) && active.has(d.key));
  }

  /** All streams that have data (for legend). */
  get availableStreams() {
    return this.streamDescriptors.filter(d => this.streamAvailable(d.hasKey));
  }

  /** Left-side active streams (for Y-axis rendering). */
  get leftActiveStreams() {
    return this.availableActiveStreams.filter(d => d.axis === 'left');
  }

  /** Right-side active streams (for Y-axis rendering). */
  get rightActiveStreams() {
    return this.availableActiveStreams.filter(d => d.axis === 'right');
  }

  /** SVG plot-area left offset in viewBox units (55 per left axis). */
  get plotOffsetX(): number { return this.leftActiveStreams.length * 55; }

  /** SVG plot-area width in viewBox units. */
  get plotW(): number { return 800 - this.plotOffsetX - this.rightActiveStreams.length * 55; }

  readonly plotH = 200;

  onChartMouseMove(event: MouseEvent): void {
    const rect = (event.currentTarget as SVGElement).getBoundingClientRect();
    const relX = event.clientX - rect.left;
    const dist = this.streams?.distancePoints;
    if (!dist?.length) return;
    const maxDist = dist[dist.length - 1] || 1;
    const targetDist = (relX / rect.width) * maxDist;
    let closest = 0;
    let minDiff = Math.abs(dist[0] - targetDist);
    for (let i = 1; i < dist.length; i++) {
      const diff = Math.abs(dist[i] - targetDist);
      if (diff < minDiff) { minDiff = diff; closest = i; }
    }
    this.tooltipIndex = closest;
    this.tooltipPixelX = relX;
    this.cdr.detectChanges();
  }

  hoverX(idx: number): number {
    if (!this.streams) return 0;
    const dist = this.streams.distancePoints;
    return (dist[idx] / (dist[dist.length - 1] || 1)) * this.plotW;
  }

  formatPace(seconds: number): string {
    if (!seconds || seconds <= 0) return '—';
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

}

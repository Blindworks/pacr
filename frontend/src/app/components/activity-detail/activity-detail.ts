import { Component, OnInit, inject, ChangeDetectorRef, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ActivityService, ActivityStreamDto, CompletedTraining, ActivityMetrics, ActivityVo2Max, GpsStreamDto } from '../../services/activity.service';
import { ActivityMapComponent } from '../activity-map/activity-map';
import { MapDialogComponent } from '../map-dialog/map-dialog';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-activity-detail',
  standalone: true,
  imports: [CommonModule, ActivityMapComponent, MapDialogComponent, RouterModule],
  templateUrl: './activity-detail.html',
  styleUrl: './activity-detail.scss'
})
export class ActivityDetail implements OnInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly activityService = inject(ActivityService);
  protected readonly userService = inject(UserService);

  @ViewChild('mapDialog') private mapDialog!: MapDialogComponent;

  activity: CompletedTraining | null = null;
  loading = true;
  error = false;

  streams: ActivityStreamDto | null = null;
  streamsLoading = false;
  streamsError = false;

  metrics: ActivityMetrics | null = null;
  vo2maxMetrics: ActivityVo2Max[] = [];

  gpsData: GpsStreamDto | null = null;
  mapColorMode: 'pace' | 'hr' = 'pace';

  private activityId = 0;

  // Stream registry — defines each stream's display properties
  readonly streamDescriptors = [
    { key: 'heartRate',        hasKey: 'hasHeartRate', label: 'HEART RATE', unit: 'bpm', color: '#b9f20d', axis: 'left'  as const, invert: false, dashed: false },
    { key: 'altitude',         hasKey: 'hasAltitude',  label: 'ELEVATION',  unit: 'm',   color: '#94a3b8', axis: 'left'  as const, invert: false, dashed: true  },
    { key: 'paceSecondsPerKm', hasKey: 'hasPace',      label: 'PACE',       unit: '/km', color: '#22d3ee', axis: 'left'  as const, invert: true,  dashed: false },
    { key: 'cadence',          hasKey: 'hasCadence',   label: 'CADENCE',    unit: 'spm', color: '#a78bfa', axis: 'left'  as const, invert: false, dashed: false },
    { key: 'power',            hasKey: 'hasPower',     label: 'POWER',      unit: 'W',   color: '#f97316', axis: 'left'  as const, invert: false, dashed: false },
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
        this.activityService.getMetrics(this.activityId).subscribe({
          next: (m) => { this.metrics = m; this.cdr.detectChanges(); },
          error: () => {}
        });
        this.activityService.getVo2MaxByActivity(this.activityId).subscribe({
          next: (v) => { this.vo2maxMetrics = v; this.cdr.detectChanges(); },
          error: () => {}
        });
        this.activityService.getGpsStream(this.activityId).subscribe({
          next: (g) => { this.gpsData = g; this.cdr.detectChanges(); },
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

  openMapDialog(): void {
    if (this.gpsData) {
      this.mapDialog.open(this.gpsData, this.mapColorMode);
    }
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

  get vo2maxPace(): ActivityVo2Max | null {
    return this.vo2maxMetrics.find(m => m.metricType === 'VO2MAX') ?? null;
  }

  get vo2maxHr(): ActivityVo2Max | null {
    return this.vo2maxMetrics.find(m => m.metricType === 'VO2MAX_HR_CORRECTED') ?? null;
  }

  get strain21Pct(): number {
    const v = this.metrics?.strain21;
    if (v == null) return 0;
    return Math.min((v / 21) * 100, 100);
  }

  get strain21Class(): string {
    const v = this.metrics?.strain21;
    if (v == null) return 'load-low';
    if (v < 7) return 'load-low';
    if (v < 14) return 'load-medium';
    return 'load-high';
  }

  get decouplingClass(): string {
    const v = this.metrics?.decouplingPct;
    if (v == null) return 'coupling-good';
    return v <= 5 ? 'coupling-good' : 'coupling-bad';
  }

  get hrCoverageFormatted(): string {
    const v = this.metrics?.hrDataCoverage;
    if (v == null) return '—';
    return `${Math.round(v * 100)} %`;
  }

  get efficiencyFactorFormatted(): string {
    const v = this.metrics?.efficiencyFactor;
    if (v == null) return '—';
    return v.toFixed(4);
  }

  decouplingReasonLabel(reason: string | null): string {
    const map: Record<string, string> = {
      OK: 'Berechnet',
      TOO_SHORT: 'Zu kurz',
      NO_HR: 'Kein HF',
      NO_PACE: 'Kein Pace',
      NO_STREAMS: 'Keine Streams',
    };
    return reason ? (map[reason] ?? reason) : '—';
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

  /** Builds SVG path string for one stream using a smooth cardinal spline. */
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

    const pts: { x: number; y: number }[] = [];
    for (let i = 0; i < data.length; i++) {
      const v = data[i];
      if (v === null || v === undefined) continue;
      const x = (dist[i] / maxDist) * plotW;
      const norm = invert ? (range.max - v) / span : (v - range.min) / span;
      pts.push({ x, y: plotH - norm * plotH });
    }

    if (pts.length < 2) return '';

    // Cardinal spline with tension 0.3
    const t = 0.3;
    let d = `M${pts[0].x.toFixed(1)},${pts[0].y.toFixed(1)}`;
    for (let i = 0; i < pts.length - 1; i++) {
      const p0 = pts[Math.max(0, i - 1)];
      const p1 = pts[i];
      const p2 = pts[i + 1];
      const p3 = pts[Math.min(pts.length - 1, i + 2)];
      const cp1x = p1.x + (p2.x - p0.x) * t;
      const cp1y = p1.y + (p2.y - p0.y) * t;
      const cp2x = p2.x - (p3.x - p1.x) * t;
      const cp2y = p2.y - (p3.y - p1.y) * t;
      d += ` C${cp1x.toFixed(1)},${cp1y.toFixed(1)} ${cp2x.toFixed(1)},${cp2y.toFixed(1)} ${p2.x.toFixed(1)},${p2.y.toFixed(1)}`;
    }
    return d;
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

  readonly plotOffsetX = 60;
  readonly plotW = 880;
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

  get xAxisTicks(): { label: string; x: number }[] {
    if (!this.streams?.distancePoints?.length) return [];
    const pts = this.streams.distancePoints;
    const maxDist = pts[pts.length - 1];
    const intervals = [0.5, 1, 2, 5, 10, 20, 50];
    const interval = intervals.find(i => maxDist / i <= 7) ?? 50;
    const ticks: { label: string; x: number }[] = [];
    for (let d = 0; d <= maxDist + 0.001; d += interval) {
      const clamped = Math.min(d, maxDist);
      ticks.push({ label: `${clamped.toFixed(0)} KM`, x: (clamped / maxDist) * this.plotW });
    }
    if (ticks.length > 0 && ticks[ticks.length - 1].x < this.plotW - 10) {
      ticks.push({ label: `${maxDist.toFixed(2)} KM`, x: this.plotW });
    }
    return ticks;
  }

  get yAxisItems(): { color: string; ticks: { y: number; label: string }[] }[] {
    return this.availableActiveStreams.map(d => {
      const data = this.getStreamData(d.key);
      if (!data) return null;
      const range = this.streamRange(data);
      if (!range) return null;
      const ticks = [0, 0.5, 1].map(frac => {
        const val = range.min + frac * (range.max - range.min);
        const y = d.invert
          ? this.plotH - ((range.max - val) / (range.max - range.min || 1)) * this.plotH
          : this.plotH - frac * this.plotH;
        const label = d.key === 'paceSecondsPerKm' ? this.formatPace(Math.round(val)) : Math.round(val).toString();
        return { y, label };
      });
      return { color: d.color, ticks };
    }).filter(Boolean) as { color: string; ticks: { y: number; label: string }[] }[];
  }

  hoverDotY(key: string, invert: boolean): number | null {
    if (this.tooltipIndex === null || !this.streams) return null;
    const data = this.getStreamData(key);
    if (!data) return null;
    const v = data[this.tooltipIndex];
    if (v === null || v === undefined) return null;
    const range = this.streamRange(data);
    if (!range) return null;
    const span = range.max - range.min || 1;
    const norm = invert ? (range.max - v) / span : (v - range.min) / span;
    return this.plotH - norm * this.plotH;
  }

  formatPace(seconds: number): string {
    if (!seconds || seconds <= 0) return '—';
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

}

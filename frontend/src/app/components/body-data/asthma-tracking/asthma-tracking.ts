import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AsthmaService, AsthmaEntry, BioWeatherDto } from '../../../services/asthma.service';
import { ProOverlay } from '../../shared/pro-overlay/pro-overlay';

interface ChartSlot {
  label: string;
  value: number | null;
  height: number;
}

@Component({
  selector: 'app-asthma-tracking',
  standalone: true,
  imports: [CommonModule, FormsModule, ProOverlay],
  templateUrl: './asthma-tracking.html',
  styleUrl: './asthma-tracking.scss'
})
export class AsthmaTracking implements OnInit {
  bioWeather = signal<BioWeatherDto | null>(null);
  entries = signal<AsthmaEntry[]>([]);
  last7Days = signal<AsthmaEntry[]>([]);

  selectedSymptoms = new Set<string>();
  severityScore = 4;
  peakFlow: number | null = null;
  inhalerUsage: 'NONE' | 'RESCUE' | 'CONTROLLER' = 'NONE';
  notes = '';
  saving = false;
  saveSuccess = false;

  readonly todayFormatted: string;

  readonly symptomOptions = [
    { key: 'SHORTNESS_OF_BREATH', label: 'Shortness of Breath' },
    { key: 'WHEEZING',            label: 'Wheezing' },
    { key: 'COUGHING',            label: 'Coughing' },
    { key: 'CHEST_TIGHTNESS',     label: 'Chest Tightness' },
  ];

  readonly inhalerOptions: { value: 'NONE' | 'RESCUE' | 'CONTROLLER'; label: string }[] = [
    { value: 'NONE',       label: 'None' },
    { value: 'RESCUE',     label: 'Rescue' },
    { value: 'CONTROLLER', label: 'Controller' },
  ];

  constructor(private asthmaService: AsthmaService) {
    const now = new Date();
    this.todayFormatted = now.toLocaleDateString('en-US', {
      weekday: 'long', month: 'long', day: 'numeric'
    });
  }

  ngOnInit(): void {
    this.loadData();
  }

  private loadData(): void {
    this.asthmaService.getEnvironment().subscribe({
      next: (data) => this.bioWeather.set(data),
      error: () => {}
    });
    this.asthmaService.getEntries().subscribe({
      next: (data) => this.entries.set(data),
      error: () => {}
    });
    this.asthmaService.getLast7Days().subscribe({
      next: (data) => this.last7Days.set(data),
      error: () => {}
    });
  }

  toggleSymptom(key: string): void {
    if (this.selectedSymptoms.has(key)) {
      this.selectedSymptoms.delete(key);
    } else {
      this.selectedSymptoms.add(key);
    }
  }

  isSymptomSelected(key: string): boolean {
    return this.selectedSymptoms.has(key);
  }

  setInhaler(val: 'NONE' | 'RESCUE' | 'CONTROLLER'): void {
    this.inhalerUsage = val;
  }

  onSubmit(): void {
    if (this.saving) return;
    this.saving = true;
    const entry: Partial<AsthmaEntry> = {
      symptoms:     Array.from(this.selectedSymptoms).join(','),
      severityScore: this.severityScore,
      peakFlowLMin:  this.peakFlow,
      inhalerUsage:  this.inhalerUsage,
      notes:         this.notes,
    };
    this.asthmaService.createEntry(entry).subscribe({
      next: () => {
        this.saving = false;
        this.saveSuccess = true;
        this.resetForm();
        this.loadData();
        setTimeout(() => (this.saveSuccess = false), 3000);
      },
      error: () => { this.saving = false; }
    });
  }

  deleteEntry(id: number): void {
    this.asthmaService.deleteEntry(id).subscribe({
      next: () => this.loadData(),
      error: () => {}
    });
  }

  // Pollen 0-3 → bar width %
  pollenBarWidth(value: number | null): number {
    if (value == null || value < 0) return 0;
    const map = [5, 38, 68, 95];
    return map[Math.min(value, 3)];
  }

  pollenLabel(value: number | null): string {
    if (value == null || value < 0) return 'None';
    switch (value) {
      case 0: return 'None';
      case 1: return 'Low';
      case 2: return 'Medium';
      case 3: return 'High';
      default: return 'None';
    }
  }

  pollenBarClass(value: number | null): string {
    if (value == null || value <= 0) return 'bar-none';
    switch (value) {
      case 1: return 'bar-low';
      case 2: return 'bar-medium';
      case 3: return 'bar-high';
      default: return 'bar-none';
    }
  }

  pollenLabelClass(value: number | null): string {
    if (value == null || value <= 0) return 'lbl-none';
    switch (value) {
      case 1: return 'lbl-low';
      case 2: return 'lbl-medium';
      case 3: return 'lbl-high';
      default: return 'lbl-none';
    }
  }

  get riskBadge(): string {
    const bw = this.bioWeather();
    if (!bw) return '';
    const idx = bw.asthmaRiskIndex;
    if (idx != null) {
      if (idx >= 60) return 'ELEVATED RISK';
      if (idx >= 30) return 'MODERATE RISK';
      return 'LOW RISK';
    }
    const maxP = Math.max(
      bw.pollenBirch ?? 0, bw.pollenGrasses ?? 0, bw.pollenMugwort ?? 0,
      bw.pollenHazel ?? 0, bw.pollenAlder ?? 0, bw.pollenAsh ?? 0
    );
    if (maxP >= 3) return 'ELEVATED RISK';
    if (maxP >= 2) return 'MODERATE RISK';
    return 'LOW RISK';
  }

  get riskBadgeClass(): string {
    const b = this.riskBadge;
    if (b.includes('ELEVATED')) return 'risk-elevated';
    if (b.includes('MODERATE')) return 'risk-moderate';
    return 'risk-low';
  }

  get riskIndexBarWidth(): number {
    return this.bioWeather()?.asthmaRiskIndex ?? 0;
  }

  get riskIndexLabel(): string {
    const idx = this.bioWeather()?.asthmaRiskIndex;
    if (idx == null) return '—';
    if (idx < 20)  return 'Sehr niedrig';
    if (idx < 40)  return 'Niedrig';
    if (idx < 60)  return 'Mittel';
    if (idx < 80)  return 'Hoch';
    return 'Sehr hoch';
  }

  get riskIndexLabelClass(): string {
    const idx = this.bioWeather()?.asthmaRiskIndex;
    if (idx == null || idx < 30) return 'lbl-low';
    if (idx < 60) return 'lbl-medium';
    return 'lbl-high';
  }

  get pm25Label(): string {
    const v = this.bioWeather()?.pm25;
    if (v == null) return '—';
    return v.toFixed(1) + ' µg/m³';
  }

  get pm25LabelClass(): string {
    const v = this.bioWeather()?.pm25;
    if (v == null || v <= 10) return 'lbl-low';
    if (v <= 25) return 'lbl-medium';
    return 'lbl-high';
  }

  get ozoneLabel(): string {
    const v = this.bioWeather()?.ozone;
    if (v == null) return '—';
    return Math.round(v) + ' µg/m³';
  }

  get ozoneLabelClass(): string {
    const v = this.bioWeather()?.ozone;
    if (v == null || v < 90) return 'lbl-low';
    if (v < 140) return 'lbl-medium';
    return 'lbl-high';
  }

  get humidityLabel(): string {
    const v = this.bioWeather()?.humidity;
    return v != null ? v + ' %' : '—';
  }

  get humidityLabelClass(): string {
    const v = this.bioWeather()?.humidity;
    if (v == null) return 'lbl-none';
    if (v < 30 || v > 80) return 'lbl-high';
    if (v < 40 || v > 70) return 'lbl-medium';
    return 'lbl-low';
  }

  get temperatureLabel(): string {
    const v = this.bioWeather()?.temperature;
    return v != null ? v.toFixed(1) + ' °C' : '—';
  }

  get temperatureLabelClass(): string {
    const v = this.bioWeather()?.temperature;
    if (v == null) return 'lbl-none';
    if (v < 0 || v > 30) return 'lbl-high';
    if (v < 8 || v > 25) return 'lbl-medium';
    return 'lbl-low';
  }

  get chart7DaySlots(): ChartSlot[] {
    const days = this.last7Days();
    const maxVal = days.reduce((m, e) => Math.max(m, e.peakFlowLMin ?? 0), 1) || 600;
    const dayLabels = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const now = new Date();
    const slots: ChartSlot[] = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date(now);
      d.setDate(d.getDate() - i);
      const dStr = new Intl.DateTimeFormat('sv-SE').format(d);
      const entry = days.find(e => e.loggedAt?.startsWith(dStr));
      const val = entry?.peakFlowLMin ?? null;
      slots.push({
        label:  i === 0 ? 'Today' : dayLabels[d.getDay()],
        value:  val,
        height: val ? Math.round((val / maxVal) * 100) : 0
      });
    }
    return slots;
  }

  getEntryLabel(entry: AsthmaEntry): string {
    if (!entry.loggedAt) return 'Log Entry';
    const h = new Date(entry.loggedAt).getHours();
    if (h < 12) return 'Morning Log';
    if (h < 17) return 'Afternoon Log';
    if (h < 21) return 'Evening Log';
    return 'Late Night Log';
  }

  formatEntryDate(dateStr: string | undefined): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const month = d.toLocaleDateString('en-US', { month: 'short' }).toUpperCase();
    const day   = d.getDate();
    const time  = d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true });
    return `${month} ${day} • ${time}`;
  }

  peakFlowColorClass(entry: AsthmaEntry): string {
    if (entry.severityScore >= 7) return 'flow-red';
    return 'flow-lime';
  }

  get avgSeverity(): string {
    const days = this.last7Days();
    if (!days.length) return '0.0';
    return (days.reduce((a, e) => a + e.severityScore, 0) / days.length).toFixed(1);
  }

  get inhalerUsageCount(): number {
    return this.last7Days().filter(e => e.inhalerUsage !== 'NONE').length;
  }

  get weeklyInsightText(): string {
    const days = this.last7Days();
    if (!days.length) return 'Log your symptoms daily to unlock personalized weekly insights.';
    const avg = parseFloat(this.avgSeverity);
    if (avg >= 7) return 'Your symptoms were significantly elevated this week. Avoid peak pollen hours (10am–3pm) and consider indoor exercise.';
    if (avg >= 4) return 'Moderate symptom load this week. Monitor pollen levels before outdoor workouts.';
    return 'Your breathing capacity is higher on low-pollen days. Schedule intense training sessions when pollen counts are low.';
  }

  private resetForm(): void {
    this.selectedSymptoms.clear();
    this.severityScore = 4;
    this.peakFlow = null;
    this.inhalerUsage = 'NONE';
    this.notes = '';
  }
}

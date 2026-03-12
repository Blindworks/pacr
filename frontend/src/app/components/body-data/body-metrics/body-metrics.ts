import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

interface SummaryCard {
  label: string;
  value: string;
  unit: string;
  change: string;
  trend: 'up' | 'down' | 'flat';
}

interface DetailMetric {
  icon: string;
  label: string;
  value: string;
  change: string;
  trend: 'up' | 'down' | 'flat';
}

interface HistoryItem {
  date: string;
  time: string;
  weight: string;
  bodyFat: string;
}

@Component({
  selector: 'app-body-metrics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './body-metrics.html',
  styleUrl: './body-metrics.scss'
})
export class BodyMetrics {
  search = '';
  private readonly router = inject(Router);

  readonly summaryCards: SummaryCard[] = [
    { label: 'Weight', value: '82.4', unit: 'kg', change: '0.5%', trend: 'down' },
    { label: 'Body Fat', value: '14.2', unit: '%', change: '0.2%', trend: 'down' },
    { label: 'Muscle Mass', value: '38.5', unit: 'kg', change: '0.5kg', trend: 'up' },
    { label: 'BMI', value: '24.1', unit: '', change: '0.0', trend: 'flat' },
  ];

  readonly detailMetrics: DetailMetric[] = [
    { icon: 'straighten', label: 'Waist Circ.', value: '84 cm', change: '-1.2cm', trend: 'down' },
    { icon: 'favorite', label: 'Resting HR', value: '54 bpm', change: '-2 bpm', trend: 'up' },
    { icon: 'blood_pressure', label: 'Blood Pressure', value: '118/76', change: 'Stable', trend: 'flat' },
    { icon: 'water_drop', label: 'Water %', value: '62.8%', change: '+0.4%', trend: 'up' },
    { icon: 'skeleton', label: 'Bone Mass', value: '3.2 kg', change: '0.0kg', trend: 'flat' },
    { icon: 'monitor_weight', label: 'Visceral Fat', value: 'Level 6', change: '-1 lvl', trend: 'up' },
  ];

  readonly historyItems: HistoryItem[] = [
    { date: 'Dec 20, 2023', time: '08:45 AM', weight: '82.4 kg', bodyFat: '14.2%' },
    { date: 'Dec 13, 2023', time: '09:12 AM', weight: '82.9 kg', bodyFat: '14.4%' },
    { date: 'Dec 06, 2023', time: '07:55 AM', weight: '83.1 kg', bodyFat: '14.5%' },
    { date: 'Nov 29, 2023', time: '08:30 AM', weight: '83.5 kg', bodyFat: '14.8%' },
  ];

  goToLogNewData(): void {
    this.router.navigate(['/body-data/log-body-metrics']);
  }
}

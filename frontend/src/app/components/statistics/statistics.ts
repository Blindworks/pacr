import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

type MonthlyBar = {
  month: string;
  currentHeight: number;
  previousHeight: number;
};

type Best = {
  label: string;
  value: string;
};

@Component({
  selector: 'app-statistics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './statistics.html',
  styleUrl: './statistics.scss'
})
export class Statistics {
  protected readonly monthlyBars: MonthlyBar[] = [
    { month: 'Jan', currentHeight: 65, previousHeight: 40 },
    { month: 'Feb', currentHeight: 70, previousHeight: 55 },
    { month: 'Mar', currentHeight: 50, previousHeight: 45 },
    { month: 'Apr', currentHeight: 40, previousHeight: 30 },
    { month: 'May', currentHeight: 90, previousHeight: 80 },
    { month: 'Jun', currentHeight: 85, previousHeight: 70 }
  ];

  protected readonly intensityZones = [
    { label: 'Easy (Zone 1-2)', value: 65, colorClass: 'zone--easy' },
    { label: 'Tempo (Zone 3)', value: 20, colorClass: 'zone--tempo' },
    { label: 'Threshold (Zone 4)', value: 10, colorClass: 'zone--threshold' },
    { label: 'Anaerobic (Zone 5)', value: 5, colorClass: 'zone--anaerobic' }
  ];

  protected readonly personalBests: Best[] = [
    { label: '5 Kilometers', value: '18:42' },
    { label: '10 Kilometers', value: '39:15' },
    { label: 'Half Marathon', value: '1:24:08' },
    { label: 'Full Marathon', value: '2:58:33' }
  ];
}

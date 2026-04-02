import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { CycleSettingsService, CycleSettings, CycleStatusDto } from '../../../services/cycle-settings.service';
import { ProOverlay } from '../../shared/pro-overlay/pro-overlay';

@Component({
  selector: 'app-cycle-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, ProOverlay, TranslateModule],
  templateUrl: './cycle-settings.html',
  styleUrl: './cycle-settings.scss'
})
export class CycleSettingsComponent implements OnInit {
  firstDayOfLastPeriod = '';
  averageCycleLength = 28;
  averagePeriodDuration = 5;

  status: CycleStatusDto | null = null;
  saveSuccess = false;
  saveError = false;

  readonly periodDurationOptions = [
    { label: '3–4 Tage', value: 3 },
    { label: '5–6 Tage', value: 5 },
    { label: '7–8 Tage', value: 7 },
  ];

  readonly phaseLabels: Record<string, string> = {
    menstrual: 'BODY_DATA.PHASE_MENSTRUAL',
    follicular: 'BODY_DATA.PHASE_FOLLICULAR',
    ovulation: 'BODY_DATA.PHASE_OVULATION',
    luteal: 'BODY_DATA.PHASE_LUTEAL',
  };

  constructor(
    private router: Router,
    private cycleSettingsService: CycleSettingsService
  ) {}

  ngOnInit(): void {
    this.cycleSettingsService.getSettings().subscribe({
      next: (s) => {
        this.firstDayOfLastPeriod = s.firstDayOfLastPeriod;
        this.averageCycleLength = s.averageCycleLength;
        this.averagePeriodDuration = s.averagePeriodDuration;
      },
      error: () => { /* 404 = no settings yet, keep defaults */ }
    });

    this.cycleSettingsService.getStatus().subscribe({
      next: (s) => { this.status = s; },
      error: () => {}
    });
  }

  save(): void {
    const settings: CycleSettings = {
      firstDayOfLastPeriod: this.firstDayOfLastPeriod,
      averageCycleLength: this.averageCycleLength,
      averagePeriodDuration: this.averagePeriodDuration,
    };

    this.cycleSettingsService.saveSettings(settings).subscribe({
      next: () => {
        this.saveSuccess = true;
        this.saveError = false;
        setTimeout(() => this.router.navigate(['/body-data/cycle-tracking']), 1200);
      },
      error: () => {
        this.saveError = true;
        this.saveSuccess = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/body-data/cycle-tracking']);
  }

  get phaseBarSegments(): { phase: string; width: number }[] {
    const cl = this.averageCycleLength;
    const pd = this.averagePeriodDuration;
    const follEnd = Math.floor(cl / 2) - 2;
    const ovStart = follEnd + 1;
    const lutStart = Math.floor(cl / 2) + 1;

    return [
      { phase: 'menstrual', width: (pd / cl) * 100 },
      { phase: 'follicular', width: ((follEnd - pd) / cl) * 100 },
      { phase: 'ovulation', width: (2 / cl) * 100 },
      { phase: 'luteal', width: ((cl - lutStart + 1) / cl) * 100 },
    ];
  }
}

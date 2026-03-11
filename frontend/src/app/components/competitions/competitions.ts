import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeStyle } from '@angular/platform-browser';
import { CompetitionService, Competition } from '../../services/competition.service';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

interface Race {
  id: number;
  tag: string;
  tagClass: string;
  date: string;
  name: string;
  distance: string;
  location: string;
  image: string;
}

interface TrainingPlan {
  id: number;
  icon: string;
  level: string;
  levelClass: string;
  title: string;
  description: string;
  commitment: string;
  peakMileage: string;
  keyFocus: string;
  targetTime: string;
  recommended: boolean;
}

@Component({
  selector: 'app-competitions',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './competitions.html',
  styleUrl: './competitions.scss'
})
export class Competitions implements OnInit {
  private competitionService = inject(CompetitionService);
  private sanitizer = inject(DomSanitizer);

  activeFilter = 'All Races';
  selectedRace: Race | null = null;
  selectedPlan: TrainingPlan | null = null;
  selectedTargetTime = 'Sub 3:30';
  isLoading = false;
  hasError = false;

  filters = ['All Races', 'Marathon', 'Half Marathon', '10K', '5K', 'Ultra'];

  targetTimes = ['Sub 3:30', 'Sub 4:00', 'Sub 4:30', 'Just Finish'];

  races: Race[] = [];

  trainingPlans: TrainingPlan[] = [
    {
      id: 1,
      icon: 'speed',
      level: 'Advanced',
      levelClass: 'level-advanced',
      title: 'Marathon Mastery: Sub 3:30',
      description: 'Designed for experienced runners looking to break their PR.',
      commitment: '5-6 sessions/wk',
      peakMileage: '85 km/wk',
      keyFocus: 'Speed & Lactate Threshold',
      targetTime: 'Sub 3:30',
      recommended: false
    },
    {
      id: 2,
      icon: 'directions_run',
      level: 'Intermediate',
      levelClass: 'level-intermediate',
      title: 'Pacing Hero: Sub 4:00',
      description: 'The gold standard for the recreational marathoner.',
      commitment: '4-5 sessions/wk',
      peakMileage: '65 km/wk',
      keyFocus: 'Endurance & Pacing',
      targetTime: 'Sub 4:00',
      recommended: true
    },
    {
      id: 3,
      icon: 'heart_check',
      level: 'Balanced',
      levelClass: 'level-balanced',
      title: 'Steady Finisher: Sub 4:30',
      description: 'Focus on consistent aerobic base and recovery protocols.',
      commitment: '3-4 sessions/wk',
      peakMileage: '50 km/wk',
      keyFocus: 'Consistency & Aerobic Base',
      targetTime: 'Sub 4:30',
      recommended: false
    },
    {
      id: 4,
      icon: 'flag',
      level: 'Beginner',
      levelClass: 'level-beginner',
      title: 'First Finisher: Just Complete It',
      description: 'Built for first-timers — cross that finish line strong.',
      commitment: '3 sessions/wk',
      peakMileage: '40 km/wk',
      keyFocus: 'Endurance & Confidence',
      targetTime: 'Just Finish',
      recommended: false
    }
  ];

  ngOnInit(): void {
    this.loadCompetitions();
  }

  private loadCompetitions(): void {
    this.isLoading = true;
    this.hasError = false;
    this.competitionService.getAll().pipe(
      catchError(() => { this.hasError = true; return of([]); }),
      finalize(() => { this.isLoading = false; })
    ).subscribe(data => {
      try {
        this.races = data.map(c => this.mapToRace(c));
      } catch (e) {
        console.error('Error mapping competitions:', e);
        this.hasError = true;
      }
    });
  }

  private mapToRace(c: Competition): Race {
    return {
      id: c.id,
      tag: c.type ?? 'Race',
      tagClass: this.typeToTagClass(c.type),
      date: this.formatDate(c.date),
      name: c.name,
      distance: this.typeToDistance(c.type),
      location: c.location ?? '',
      image: this.typeToImage(c.type)
    };
  }

  private typeToImage(type?: string): string {
    const marathon = 'https://lh3.googleusercontent.com/aida-public/AB6AXuCY0ob1wpMOeP6zfuLA-xzjVtIckonf1GuCS_vGNUyyaacDNeAeBAcVGWSPihwW0E5Y3I6KnQudalx5St9WOeVKQHfpL83DsrKqMhdRgiOVftcCzliFs_aUf_DgHpwryCGJ1EVblEq5skgr3OmkcR-R97nHWGy45KTvb95LVqrOkqO4cWsk0cGDmvBeHubPRM9V99muuux592kQFfw-SIrSrUH5m2deE_S5-B2ZxdEedfsgOK5asUM5jpyNLohwm5uC0_61hsPN89o';
    const cityRun = 'https://lh3.googleusercontent.com/aida-public/AB6AXuAeGUUGllRX4y_Jje09V_m_xnoRUjjpbMdcEP5Ra4R7uhJkaHsDmVreDqcwBbLdWWLp6MorUC-5F2MVBleoQcuyEGO12w--fUN3MrAha3DpIYUuvGoMC0m3WEQ6XrhOoUhsin4xzv0HEqeQ-5FUnqUswmSk-zfj9D1cA12wL9eWiv98Mlwt0VJUfP9WvTrRRUTyaWrlCKsHV220-W46K2ekA8fmzqdD8p3patR_OoDa2sFdvug9lCL4vOQRP0sYfO3Sz2aKRas5aqk';
    const map: Record<string, string> = {
      'Marathon': marathon,
      'Halbmarathon': marathon,
      '10K': cityRun,
      '5K': cityRun,
      '50K': marathon,
      '100K': marathon,
      'Backyard Ultra': marathon,
      'Catcher car': marathon
    };
    return map[type ?? ''] ?? cityRun;
  }

  getImageStyle(imageUrl: string): SafeStyle {
    return this.sanitizer.bypassSecurityTrustStyle(`url(${imageUrl})`);
  }

  private typeToTagClass(type?: string): string {
    const map: Record<string, string> = {
      'Marathon': 'tag-major',
      'Halbmarathon': 'tag-major',
      '10K': 'tag-city',
      '5K': 'tag-city',
      '50K': 'tag-ultra',
      '100K': 'tag-ultra',
      'Backyard Ultra': 'tag-ultra',
      'Catcher car': 'tag-ultra'
    };
    return map[type ?? ''] ?? 'tag-other';
  }

  private typeToDistance(type?: string): string {
    const map: Record<string, string> = {
      'Marathon': '42.2 km',
      'Halbmarathon': '21.1 km',
      '10K': '10.0 km',
      '5K': '5.0 km',
      '50K': '50.0 km',
      '100K': '100.0 km',
      'Backyard Ultra': 'varies',
      'Catcher car': 'varies'
    };
    return map[type ?? ''] ?? '—';
  }

  private formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('de-DE', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  get filteredRaces(): Race[] {
    if (this.activeFilter === 'All Races') return this.races;
    const filterMap: Record<string, string[]> = {
      'Marathon': ['Marathon'],
      'Half Marathon': ['Halbmarathon'],
      '10K': ['10K'],
      '5K': ['5K'],
      'Ultra': ['50K', '100K', 'Backyard Ultra', 'Catcher car']
    };
    const allowed = filterMap[this.activeFilter] ?? [];
    return this.races.filter(r => allowed.includes(r.tag));
  }

  get filteredPlans(): TrainingPlan[] {
    return this.trainingPlans.filter(p => p.targetTime === this.selectedTargetTime);
  }

  setFilter(filter: string): void {
    this.activeFilter = filter;
  }

  selectRace(race: Race): void {
    this.selectedRace = race;
    this.selectedPlan = null;
    this.selectedTargetTime = 'Sub 3:30';
  }

  deselectRace(): void {
    this.selectedRace = null;
    this.selectedPlan = null;
  }

  setTargetTime(time: string): void {
    this.selectedTargetTime = time;
  }

  selectPlan(plan: TrainingPlan): void {
    this.selectedPlan = plan;
  }

  deselectPlan(): void {
    this.selectedPlan = null;
  }

  get planWeeks(): number {
    return 16;
  }

  get planTotalKm(): string {
    const peak = parseInt(this.selectedPlan?.peakMileage ?? '0', 10);
    return '~' + (peak * 10).toString();
  }
}

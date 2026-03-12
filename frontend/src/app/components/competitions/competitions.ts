import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { DomSanitizer, SafeStyle } from '@angular/platform-browser';
import { CompetitionService, Competition } from '../../services/competition.service';
import { TrainingPlanService, TrainingPlan } from '../../services/training-plan.service';


interface Race {
  id: number;
  tag: string;
  tagClass: string;
  date: string;
  name: string;
  distance: string;
  location: string;
  image: string;
  registered: boolean;
}

interface PlanCard {
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
  private trainingPlanService = inject(TrainingPlanService);
  private router = inject(Router);
  private sanitizer = inject(DomSanitizer);
  private cdr = inject(ChangeDetectorRef);

  activeFilter = 'All Races';
  selectedRace: Race | null = null;
  selectedPlan: PlanCard | null = null;
  selectedTargetTime = 'Sub 3:30';
  isLoading = false;
  hasError = false;
  isAssigning = false;
  assignError = false;

  filters = ['All Races', 'Marathon', 'Half Marathon', '10K', '5K', 'Ultra'];
  targetTimes = ['Sub 3:30', 'Sub 4:00', 'Sub 4:30', 'Just Finish'];

  races: Race[] = [];
  trainingPlans: PlanCard[] = [];
  isPlansLoading = false;

  ngOnInit(): void {
    this.loadCompetitions();
  }

  private loadCompetitions(): void {
    this.isLoading = true;
    this.hasError = false;
    this.competitionService.getAll().subscribe({
      next: (data) => {
        try {
          this.races = data.map(c => this.mapToRace(c));
        } catch (e) {
          console.error('Error mapping competitions:', e);
          this.hasError = true;
        }
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.hasError = true;
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private loadTrainingPlans(): void {
    this.isPlansLoading = true;
    this.trainingPlanService.getTemplates().subscribe({
      next: (plans) => {
        this.trainingPlans = plans.map(p => this.mapToPlanCard(p));
        this.isPlansLoading = false;
      },
      error: () => {
        this.isPlansLoading = false;
      }
    });
  }

  private mapToPlanCard(p: TrainingPlan): PlanCard {
    return {
      id: p.id,
      icon: 'directions_run',
      level: p.targetTime ?? '—',
      levelClass: 'level-intermediate',
      title: p.name,
      description: p.description ?? '',
      commitment: '—',
      peakMileage: '—',
      keyFocus: p.prerequisites ?? '—',
      targetTime: p.targetTime ?? '',
      recommended: false
    };
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
      image: this.typeToImage(c.type),
      registered: c.registered ?? false
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

  get filteredPlans(): PlanCard[] {
    if (!this.selectedTargetTime) return this.trainingPlans;
    const filtered = this.trainingPlans.filter(p => p.targetTime === this.selectedTargetTime);
    return filtered.length > 0 ? filtered : this.trainingPlans;
  }

  setFilter(filter: string): void {
    this.activeFilter = filter;
  }

  selectRace(race: Race): void {
    this.selectedRace = race;
    this.selectedPlan = null;
    this.selectedTargetTime = 'Sub 3:30';
    if (this.trainingPlans.length === 0) {
      this.loadTrainingPlans();
    }
  }

  deselectRace(): void {
    this.selectedRace = null;
    this.selectedPlan = null;
  }

  setTargetTime(time: string): void {
    this.selectedTargetTime = time;
  }

  selectPlan(plan: PlanCard): void {
    this.selectedPlan = plan;
  }

  deselectPlan(): void {
    this.selectedPlan = null;
  }

  startTrainingPlan(): void {
    if (!this.selectedRace || !this.selectedPlan) return;
    this.isAssigning = true;
    this.assignError = false;
    this.trainingPlanService.assignToCompetition(this.selectedPlan.id, this.selectedRace.id).subscribe({
      next: (result) => {
        if (result) this.router.navigate(['/training-plans']);
      },
      error: () => {
        this.assignError = true;
        this.isAssigning = false;
        this.cdr.detectChanges();
      },
      complete: () => {
        this.isAssigning = false;
        this.cdr.detectChanges();
      }
    });
  }

  get planWeeks(): number {
    return 16;
  }

  get planTotalKm(): string {
    const peak = parseInt(this.selectedPlan?.peakMileage ?? '0', 10);
    return '~' + (peak * 10).toString();
  }
}

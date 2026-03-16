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
  trainingPlanId?: number;
  trainingPlanName?: string;
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
  highlightedPlanId: number | null = null;
  selectedTargetTime = 'Sub 3:30';
  isLoading = false;
  hasError = false;
  isAssigning = false;
  assignError = false;
  showPlanChangeConfirm = false;

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
        this.syncHighlightedPlan();
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
      image: this.typeToImage(c.type, c.id),
      registered: c.registered ?? false,
      trainingPlanId: c.trainingPlanId,
      trainingPlanName: c.trainingPlanName
    };
  }

  private syncHighlightedPlan(): void {
    if (!this.selectedRace?.trainingPlanId) {
      return;
    }

    const assignedPlan = this.trainingPlans.find(plan => plan.id === this.selectedRace?.trainingPlanId);
    if (!assignedPlan) {
      return;
    }

    this.highlightedPlanId = assignedPlan.id;
    if (assignedPlan.targetTime) {
      this.selectedTargetTime = assignedPlan.targetTime;
    }
  }

  private typeToImage(type?: string, id: number = 0): string {
    const w = 'w=800&q=80';
    const u = (photoId: string) => `https://images.unsplash.com/photo-${photoId}?${w}`;

    const marathonImages = [
      u('1452626038306-9aae5e071dd3'), // Massenstart Marathon
      u('1530137073521-c10668e264d3'), // Straßenrennen große Gruppe
      u('1476480862126-209bfaa8edc8'), // Stadtmarathon Brücke
      u('1571008887538-b36bb32f4571'), // Läufer Nahaufnahme Beine
      u('1461897104016-0b3b00cc81ee'), // Marathonläufer Startnummer
    ];

    const cityRunImages = [
      u('1541534741688-6078c6bfb5c5'), // Laufbahn Sprint
      u('1552674605-db6ffd4facb5'),     // Nachtlauf Stadt
      u('1502904550040-7534597429ae'), // Laufschuhe Asphalt
      u('1546483875-ad9aa773783f'),     // Läufer Stadtpark
      u('1594737625785-a6cbdabd333c'), // Zieleinlauf Finisher
    ];

    const ultraImages = [
      u('1483721310020-03333e577078'), // Trailrunning Berge
      u('1551698618-1dfe5d97d256'),     // Trail Bergpfad
      u('1504021831741-f2cd878daa4a'), // Querfeld-/Crosslauf
      u('1490127252417-7c393f993ee4'), // Ultra Berglandschaft
      u('1446057032654-9d8885db76c6'), // Trailrunner Panorama
    ];

    const pick = (arr: string[]) => arr[Math.abs(id) % arr.length];

    const map: Record<string, string[]> = {
      'Marathon':       marathonImages,
      'Halbmarathon':   marathonImages,
      '10K':            cityRunImages,
      '5K':             cityRunImages,
      '50K':            ultraImages,
      '100K':           ultraImages,
      'Backyard Ultra': ultraImages,
      'Catcher car':    ultraImages,
    };

    return pick(map[type ?? ''] ?? cityRunImages);
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
    this.highlightedPlanId = race.trainingPlanId ?? null;
    this.selectedTargetTime = 'Sub 3:30';
    if (this.trainingPlans.length === 0) {
      this.loadTrainingPlans();
      return;
    }

    this.syncHighlightedPlan();
    if (!race.trainingPlanId) {
      this.highlightedPlanId = null;
    }
  }

  deselectRace(): void {
    this.selectedRace = null;
    this.selectedPlan = null;
    this.highlightedPlanId = null;
  }

  setTargetTime(time: string): void {
    this.selectedTargetTime = time;
  }

  selectPlan(plan: PlanCard): void {
    this.highlightedPlanId = plan.id;
    this.selectedPlan = plan;
  }

  deselectPlan(): void {
    this.highlightedPlanId = this.selectedPlan?.id ?? this.selectedRace?.trainingPlanId ?? null;
    this.selectedPlan = null;
  }

  isPlanSelected(plan: PlanCard): boolean {
    return this.highlightedPlanId === plan.id;
  }

  startTrainingPlan(): void {
    if (!this.selectedRace || !this.selectedPlan) return;
    if (this.requiresPlanChangeConfirmation()) {
      this.showPlanChangeConfirm = true;
      return;
    }
    this.assignSelectedPlan();
  }

  confirmPlanChange(): void {
    this.showPlanChangeConfirm = false;
    this.assignSelectedPlan();
  }

  cancelPlanChange(): void {
    this.showPlanChangeConfirm = false;
  }

  private requiresPlanChangeConfirmation(): boolean {
    return !!this.selectedRace?.trainingPlanId && this.selectedRace.trainingPlanId !== this.selectedPlan?.id;
  }

  private assignSelectedPlan(): void {
    if (!this.selectedRace || !this.selectedPlan) return;
    this.isAssigning = true;
    this.assignError = false;
    this.trainingPlanService.assignToCompetition(this.selectedPlan.id, this.selectedRace.id).subscribe({
      next: (result) => {
        if (result) this.router.navigate(['/training-plans']);
      },
      error: () => {
        this.assignError = true;
        this.showPlanChangeConfirm = false;
        this.isAssigning = false;
        this.cdr.detectChanges();
      },
      complete: () => {
        this.showPlanChangeConfirm = false;
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

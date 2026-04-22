import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { DomSanitizer, SafeStyle } from '@angular/platform-browser';
import { CompetitionService, Competition, CompetitionFormat } from '../../services/competition.service';
import { TrainingPlanService, TrainingPlan } from '../../services/training-plan.service';


interface RaceFormat {
  id: number;
  type: string;
  typeLabel: string;
  tagClass: string;
  startTime?: string;
  startDate?: string;
  description?: string;
  distance: string;
}

interface Race {
  id: number;
  tag: string;
  tagClass: string;
  date: string;
  rawDate: string;
  name: string;
  distance: string;
  location: string;
  image: string;
  registered: boolean;
  registeredWithOrganizer: boolean;
  trainingPlanId?: number;
  trainingPlanName?: string;
  description?: string;
  startTime?: string;
  latitude?: number;
  longitude?: number;
  organizerUrl?: string;
  formats: RaceFormat[];
  registeredFormatId?: number;
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
  imports: [CommonModule, TranslateModule],
  templateUrl: './competitions.html',
  styleUrl: './competitions.scss'
})
export class Competitions implements OnInit {
  private competitionService = inject(CompetitionService);
  private trainingPlanService = inject(TrainingPlanService);
  private router = inject(Router);
  private sanitizer = inject(DomSanitizer);
  private cdr = inject(ChangeDetectorRef);
  private readonly translate = inject(TranslateService);

  activeFilter = 'All Races';
  showPastRaces = false;
  selectedRace: Race | null = null;
  selectedFormat: RaceFormat | null = null;
  selectedPlan: PlanCard | null = null;
  highlightedPlanId: number | null = null;
  selectedTargetTime = 'Sub 3:30';
  isLoading = false;
  hasError = false;
  isAssigning = false;
  assignError = false;
  isRegistering = false;
  showPlanChangeConfirm = false;
  infoRace: Race | null = null;

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
          this.races = data.map(c => this.mapToRace(c))
            .sort((a, b) => a.rawDate.localeCompare(b.rawDate));
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
    const formats: RaceFormat[] = (c.formats || []).map(f => ({
      id: f.id!,
      type: f.type,
      typeLabel: this.typeToLabel(f.type),
      tagClass: this.typeToTagClass(this.typeToLabel(f.type)),
      startTime: f.startTime,
      startDate: f.startDate,
      description: f.description,
      distance: this.typeToDistance(this.typeToLabel(f.type))
    }));

    // For display: use first format's type if formats exist, otherwise fallback to competition type
    const primaryType = formats.length > 0 ? this.typeToLabel(formats[0].type) : c.type;

    return {
      id: c.id,
      tag: primaryType ?? 'Race',
      tagClass: this.typeToTagClass(primaryType),
      date: this.formatDate(c.date),
      rawDate: c.date ?? '',
      name: c.name,
      distance: formats.length === 1 ? formats[0].distance : this.typeToDistance(c.type),
      location: c.location ?? '',
      image: this.typeToImage(primaryType, c.id),
      registered: c.registered ?? false,
      registeredWithOrganizer: c.registeredWithOrganizer ?? false,
      trainingPlanId: c.trainingPlanId,
      trainingPlanName: c.trainingPlanName,
      description: c.description,
      startTime: c.startTime,
      latitude: c.latitude,
      longitude: c.longitude,
      organizerUrl: c.organizerUrl,
      formats,
      registeredFormatId: c.registeredFormatId
    };
  }

  private typeToLabel(type?: string): string {
    const map: Record<string, string> = {
      'FIVE_K': '5K',
      'TEN_K': '10K',
      'TWENTY_K': '20K',
      'HALF_MARATHON': 'Halbmarathon',
      'THIRTY_K': '30K',
      'FORTY_K': '40K',
      'MARATHON': 'Marathon',
      'FIFTY_K': '50K',
      'HUNDRED_K': '100K',
      'BACKYARD_ULTRA': 'Backyard Ultra',
      'CATCHER_CAR': 'Catcher car',
      'OTHER': 'Sonstige'
    };
    return map[type ?? ''] ?? type ?? '';
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
    const count = 3;
    const categoryMap: Record<string, string> = {
      'Marathon': 'marathon',
      'Halbmarathon': 'marathon',
      '10K': 'city',
      '5K': 'city',
      '50K': 'ultra',
      '100K': 'ultra',
      'Backyard Ultra': 'ultra',
      'Catcher car': 'ultra',
    };

    const category = categoryMap[type ?? ''] ?? 'city';
    const index = (Math.abs(id) % count) + 1;
    return `assets/images/competitions/${category}-${index}.webp`;
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
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    let races = this.races;
    if (!this.showPastRaces) {
      races = races.filter(r => !r.rawDate || new Date(r.rawDate) >= today);
    }
    if (this.activeFilter === 'All Races') return races;
    const filterMap: Record<string, string[]> = {
      'Marathon': ['Marathon'],
      'Half Marathon': ['Halbmarathon'],
      '10K': ['10K'],
      '5K': ['5K'],
      'Ultra': ['50K', '100K', 'Backyard Ultra', 'Catcher car']
    };
    const allowed = filterMap[this.activeFilter] ?? [];
    return races.filter(r => {
      // Check if any format matches the filter
      if (r.formats.length > 0) {
        return r.formats.some(f => allowed.includes(f.typeLabel));
      }
      return allowed.includes(r.tag);
    });
  }

  get hasPastRaces(): boolean {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return this.races.some(r => r.rawDate && new Date(r.rawDate) < today);
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
    this.selectedFormat = null;
    this.highlightedPlanId = race.trainingPlanId ?? null;
    this.selectedTargetTime = 'Sub 3:30';

    // Auto-select format if only one exists
    if (race.formats.length === 1) {
      this.selectedFormat = race.formats[0];
    }

    // If no formats or format already selected, go to plan selection
    if (race.formats.length <= 1) {
      if (this.trainingPlans.length === 0) {
        this.loadTrainingPlans();
        return;
      }
      this.syncHighlightedPlan();
      if (!race.trainingPlanId) {
        this.highlightedPlanId = null;
      }
    }
    // If multiple formats, the template will show the format picker
  }

  selectFormat(format: RaceFormat): void {
    this.selectedFormat = format;
    if (this.trainingPlans.length === 0) {
      this.loadTrainingPlans();
      return;
    }
    this.syncHighlightedPlan();
    if (!this.selectedRace?.trainingPlanId) {
      this.highlightedPlanId = null;
    }
  }

  deselectFormat(): void {
    this.selectedFormat = null;
    this.selectedPlan = null;
    this.highlightedPlanId = null;
  }

  get needsFormatSelection(): boolean {
    return !!this.selectedRace && this.selectedRace.formats.length > 1 && !this.selectedFormat;
  }

  deselectRace(): void {
    this.selectedRace = null;
    this.selectedFormat = null;
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

  participateWithoutPlan(): void {
    if (!this.selectedRace || this.isRegistering) return;
    this.isRegistering = true;
    this.competitionService.register(this.selectedRace.id, this.selectedFormat?.id).subscribe({
      next: () => {
        const race = this.races.find(r => r.id === this.selectedRace?.id);
        if (race) race.registered = true;
        this.deselectRace();
        this.isRegistering = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isRegistering = false;
        this.cdr.detectChanges();
      }
    });
  }

  openInfo(race: Race): void { this.infoRace = race; }
  closeInfo(): void { this.infoRace = null; }

  toggleOrganizerRegistration(race: Race, event: Event): void {
    event.stopPropagation();
    if (!race.registered) return;
    const newValue = !race.registeredWithOrganizer;
    this.competitionService.updateRegistration(race.id, newValue).subscribe({
      next: () => {
        race.registeredWithOrganizer = newValue;
        this.cdr.detectChanges();
      }
    });
  }

  private requiresPlanChangeConfirmation(): boolean {
    return !!this.selectedRace?.trainingPlanId && this.selectedRace.trainingPlanId !== this.selectedPlan?.id;
  }

  private assignSelectedPlan(): void {
    if (!this.selectedRace || !this.selectedPlan) return;
    this.isAssigning = true;
    this.assignError = false;
    this.trainingPlanService.assignToCompetition(this.selectedPlan.id, this.selectedRace.id, this.selectedFormat?.id).subscribe({
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

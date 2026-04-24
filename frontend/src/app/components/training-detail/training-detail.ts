import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { TrainingService, Training, TrainingStep, TrainingStepBlock } from '../../services/training.service';

interface WorkoutStep {
  icon: string;
  title: string;
  subtitle: string;
  pace: string;
  measurement: string;
  highlight: boolean;
  muted: boolean;
}

interface WorkoutBlock {
  repeatCount: number;
  label: string;
  steps: WorkoutStep[];
}

interface WorkoutItem {
  type: 'step' | 'block';
  step?: WorkoutStep;
  block?: WorkoutBlock;
}

interface PrepTip {
  icon: string;
  title: string;
  text: string;
}

export interface TrainingDetailData {
  id: number;
  sessionNumber: number;
  title: string;
  difficulty: string;
  duration: string;
  intensity: string;
  calories: string;
  benefit: string;
  estimatedDistance: string;
  heroImage: string | null;
  items: WorkoutItem[];
  prepTips: PrepTip[];
}

@Component({
  selector: 'app-training-detail',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './training-detail.html',
  styleUrl: './training-detail.scss'
})
export class TrainingDetail implements OnInit {
  private readonly translate = inject(TranslateService);
  training: TrainingDetailData | null = null;

  private readonly TRAINING_TYPE_IMAGE_COUNT = 3;
  private readonly TRAINING_TYPE_CATEGORIES = [
    'recovery', 'endurance', 'speed', 'strength', 'race',
    'swimming', 'cycling', 'fartlek', 'general'
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private trainingService: TrainingService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.trainingService.getTrainingById(id).subscribe({
      next: (t: Training) => {
        this.training = this.mapToDetailData(t);
        this.cdr.detectChanges();
      },
      error: () => {
        this.training = null;
        this.cdr.detectChanges();
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/training-plans']);
  }

  private mapToDetailData(t: Training): TrainingDetailData {
    return {
      id: t.id,
      sessionNumber: t.id,
      title: t.name,
      difficulty: t.difficulty || '—',
      duration: `${t.durationMinutes || 0} min`,
      intensity: t.intensityScore
        ? `High (${t.intensityScore}/10)`
        : (t.intensityLevel || 'Medium'),
      calories: t.estimatedCalories ? `${t.estimatedCalories} kcal` : '—',
      benefit: t.benefit || '—',
      estimatedDistance: t.estimatedDistanceMeters
        ? `${(t.estimatedDistanceMeters / 1000).toFixed(1)} km`
        : '—',
      heroImage: t.heroImageUrl || this.pickHeroImage(t.trainingType, t.id),
      items: this.mergeStepsAndBlocks(t.steps || [], t.blocks || []),
      prepTips: (t.prepTips || []).map(p => ({
        icon: p.icon || '',
        title: p.title,
        text: p.text || ''
      }))
    };
  }

  private mergeStepsAndBlocks(steps: TrainingStep[], blocks: TrainingStepBlock[]): WorkoutItem[] {
    type Tagged = { sortOrder: number; item: WorkoutItem };
    const tagged: Tagged[] = [];

    steps.forEach(s => tagged.push({
      sortOrder: s.sortOrder ?? 0,
      item: { type: 'step', step: this.mapStep(s) }
    }));

    blocks.forEach(b => tagged.push({
      sortOrder: b.sortOrder ?? 0,
      item: {
        type: 'block',
        block: {
          repeatCount: b.repeatCount ?? 2,
          label: b.label || '',
          steps: (b.steps || []).map(s => this.mapStep(s))
        }
      }
    }));

    tagged.sort((a, b) => a.sortOrder - b.sortOrder);
    return tagged.map(t => t.item);
  }

  private mapStep(s: TrainingStep): WorkoutStep {
    return {
      icon: s.icon || this.getDefaultStepIcon(s.stepType),
      title: s.title || this.formatStepTitle(s.stepType),
      subtitle: s.subtitle || '',
      pace: s.paceDisplay || '—',
      measurement: this.formatStepMeasurement(s.durationSeconds, s.durationMinutes, s.distanceMeters),
      highlight: s.highlight ?? false,
      muted: s.muted ?? false
    };
  }

  private pickHeroImage(trainingType: string | undefined, id: number): string {
    const normalized = (trainingType ?? '').toLowerCase();
    const category = this.TRAINING_TYPE_CATEGORIES.includes(normalized) ? normalized : 'general';
    const index = (Math.abs(id) % this.TRAINING_TYPE_IMAGE_COUNT) + 1;
    return `assets/images/trainings/${category}-${index}.webp`;
  }

  private getDefaultStepIcon(stepType?: string): string {
    switch (stepType) {
      case 'warmup': return 'directions_run';
      case 'work': return 'speed';
      case 'recovery': return 'pause';
      case 'cooldown': return 'ac_unit';
      case 'rest': return 'hotel';
      default: return 'fitness_center';
    }
  }

  private formatStepTitle(stepType?: string): string {
    if (!stepType) {
      return this.translate.instant('TRAINING_DETAIL.WORKOUT');
    }

    return stepType
      .split(/[-_\s]+/)
      .filter(Boolean)
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  private formatStepMeasurement(durationSeconds?: number, durationMinutes?: number, distanceMeters?: number): string {
    if (distanceMeters != null && distanceMeters > 0) {
      return `${distanceMeters} m`;
    }

    const totalSeconds = durationSeconds ?? (durationMinutes != null ? durationMinutes * 60 : null);
    if (totalSeconds == null || totalSeconds <= 0) {
      return '—';
    }

    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }
}

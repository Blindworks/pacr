import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TrainingService, Training } from '../../services/training.service';

interface WorkoutStep {
  icon: string;
  title: string;
  subtitle: string;
  pace: string;
  measurement: string;
  highlight: boolean;
  muted: boolean;
  repetitions?: number;
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
  steps: WorkoutStep[];
  prepTips: PrepTip[];
}

@Component({
  selector: 'app-training-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './training-detail.html',
  styleUrl: './training-detail.scss'
})
export class TrainingDetail implements OnInit {
  training: TrainingDetailData | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private trainingService: TrainingService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.trainingService.getTrainingById(id).subscribe({
      next: (t: Training) => {
        this.training = this.mapToDetailData(t);
      },
      error: () => {
        this.training = null;
      }
    });
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
      heroImage: t.heroImageUrl || null,
      steps: (t.steps || []).map(s => ({
        icon: s.icon || '',
        title: s.title || this.formatStepTitle(s.stepType),
        subtitle: s.subtitle || '',
        pace: s.paceDisplay || '—',
        measurement: this.formatStepMeasurement(s.durationSeconds, s.durationMinutes, s.distanceMeters),
        highlight: s.highlight ?? false,
        muted: s.muted ?? false,
        repetitions: s.repetitions
      })),
      prepTips: (t.prepTips || []).map(p => ({
        icon: p.icon || '',
        title: p.title,
        text: p.text || ''
      }))
    };
  }

  goBack(): void {
    this.router.navigate(['/training-plans']);
  }

  private formatStepTitle(stepType?: string): string {
    if (!stepType) {
      return 'Step';
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

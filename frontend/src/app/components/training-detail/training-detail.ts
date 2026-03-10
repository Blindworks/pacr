import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TrainingService, Training } from '../../services/training.service';

interface WorkoutStep {
  icon: string;
  title: string;
  subtitle: string;
  pace: string;
  highlight: boolean;
  muted: boolean;
  repetitions?: number;
  duration?: number;
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
  difficulty: string;  // e.g. 'Advanced'
  duration: string;    // e.g. '45 min'
  intensity: string;   // e.g. 'High (8/10)'
  calories: string;    // e.g. '540 kcal'
  benefit: string;     // e.g. 'VO2 Max'
  estimatedDistance: string; // e.g. '8.2 km'
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
      duration: `${t.duration || 0} min`,
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
        title: s.title,
        subtitle: s.subtitle || '',
        pace: s.paceDisplay || '—',
        highlight: s.highlight ?? false,
        muted: s.muted ?? false,
        repetitions: s.repetitions,
        duration: s.durationMinutes
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
}

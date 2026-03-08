import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';

interface WorkoutStep {
  icon: string;
  title: string;
  subtitle: string;
  pace: string;
  highlight: boolean;
  muted: boolean;
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
  heroImage: string;
  steps: WorkoutStep[];
  prepTips: PrepTip[];
}

const TRAININGS: TrainingDetailData[] = [
  {
    id: 1,
    sessionNumber: 40,
    title: 'Recovery Run',
    difficulty: 'Easy',
    duration: '32 min',
    intensity: 'Low (3/10)',
    calories: '320 kcal',
    benefit: 'Recovery',
    estimatedDistance: '5.0 km',
    heroImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBzhJYjjcQoTEoemxeKBVR33EvCaptA2wIPhERpIqDqbJbPBso3ccYP42UY4nbptfrYJa7cOBhfuMl-5vpF0Gg-9RpJyVEK4bfyrCNWlixF6uP3ExS7gWSRSfmHiMMWpqppyPvdxVZEgawJLwWb5_UiFq1nEMI_pUEYxnRnhy2YSx4NGoUUZhXJHxHNqKICFi-Vso1T1gxLrlG4qf5pYXZP5td4AZkGgzXhz7k2kaDBz_6a1u7A0Rqb4Czizk0a1pe9plcUYJNCtJk',
    steps: [
      { icon: 'fitness_center', title: 'Warm-up Walk', subtitle: '5 min • Brisk Walk', pace: '—', highlight: false, muted: false },
      { icon: 'directions_run', title: 'Easy Jog', subtitle: '22 min • Zone 2', pace: '6:32/km', highlight: true, muted: false },
      { icon: 'mode_cool_off', title: 'Cool-down Walk', subtitle: '5 min • Slow Walk', pace: '—', highlight: false, muted: false },
    ],
    prepTips: [
      { icon: 'restaurant', title: 'Pre-workout Fuel', text: 'Light snack or nothing — this is a low-intensity session.' },
      { icon: 'water_drop', title: 'Hydration', text: 'Normal daily hydration is sufficient.' },
      { icon: 'psychology', title: 'Mental Strategy', text: 'Keep it easy — resist the urge to push the pace.' },
      { icon: 'foot_bones', title: 'Gear', text: 'Comfortable daily trainers work perfectly for this session.' },
    ]
  },
  {
    id: 2,
    sessionNumber: 42,
    title: 'Threshold Intervals',
    difficulty: 'Advanced',
    duration: '45 min',
    intensity: 'High (8/10)',
    calories: '540 kcal',
    benefit: 'VO2 Max',
    estimatedDistance: '8.2 km',
    heroImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCT-0EkC-X_TV5sAGTnCYVv0ZMuHws30Z0BJrgUOIzIaE8Smd0IgQELmbFyNJQk5ZQWJd1_tg3OpDN8hvj1XJUFxRAVniIO1qY819I682jPf_n-g4D3sNyIsioMC_qHyTZG7fKsZzipOVKWNrPWtqU8nhV5IiVwsEgbuo5BQ5bw2_jC7jk-znYFfVdlCHRHGhr81bWVA_fsMWY6SMIV-CVq5z888lMPKI78XccGOXB9IiCgkSIdR7gCq_BoYn1tFbnczbT_uuFnbzo',
    steps: [
      { icon: 'fitness_center', title: 'Warm-up', subtitle: '10 min • Easy Jog • Zone 2', pace: '05:45/km', highlight: false, muted: false },
      { icon: 'sprint', title: '4 x 5min Intervals', subtitle: '20 min • Threshold • Zone 4', pace: '04:15/km', highlight: true, muted: false },
      { icon: 'replay', title: '90s Recovery', subtitle: 'After each interval • Slow Walk', pace: '—', highlight: false, muted: true },
      { icon: 'mode_cool_off', title: 'Cool-down', subtitle: '10 min • Dynamic Stretch', pace: '06:00/km', highlight: false, muted: false },
    ],
    prepTips: [
      { icon: 'restaurant', title: 'Pre-workout Fuel', text: 'Consume 30g of fast-acting carbs 45 mins before starting. Avoid heavy fats.' },
      { icon: 'water_drop', title: 'Hydration', text: 'Aim for 500ml of water with electrolytes during the 2 hours before the run.' },
      { icon: 'foot_bones', title: 'Gear Recommendation', text: 'Carbon plated or lightweight tempo shoes recommended for this intensity.' },
      { icon: 'psychology', title: 'Mental Strategy', text: "Focus on the 3rd interval — it's usually the hardest. Break it into 1-minute blocks." },
    ]
  },
  {
    id: 4,
    sessionNumber: 43,
    title: 'Aerobic Base Run',
    difficulty: 'Moderate',
    duration: '55 min',
    intensity: 'Medium (5/10)',
    calories: '620 kcal',
    benefit: 'Endurance',
    estimatedDistance: '10.0 km',
    heroImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAhSLEmQhoX6CkmeCJeY1ujwgW-YQSZfky00vBXuFTugss8Oxv_BGtfj4u7tWQnQIasayoD_b6d5Gd7r64w9NQfmzE9oCkvZlxnERpnOlit2vDd8St1KdrxVf7i-Kypbr8rGtEMoegRuNvLTHP8PPAODOq1u5XhymtW3e8HTxrmNM22RPG0SbvmkgDMNvqw05GYYDbX_4pCrh0msot-p7U_m_ZDYHJDb0SQoEi3QITGZhFoTRrlb0-9PwVVuOcKWZT1O01gv3OpxXg',
    steps: [
      { icon: 'fitness_center', title: 'Warm-up', subtitle: '5 min • Easy pace', pace: '06:10/km', highlight: false, muted: false },
      { icon: 'directions_run', title: 'Zone 2 Run', subtitle: '45 min • Aerobic • Zone 2', pace: '05:30/km', highlight: true, muted: false },
      { icon: 'mode_cool_off', title: 'Cool-down', subtitle: '5 min • Walk', pace: '—', highlight: false, muted: false },
    ],
    prepTips: [
      { icon: 'restaurant', title: 'Pre-workout Fuel', text: 'A light meal 1–2 hours before is ideal for a longer aerobic run.' },
      { icon: 'water_drop', title: 'Hydration', text: 'Carry water if it\'s warm, or plan a water stop at the halfway point.' },
      { icon: 'foot_bones', title: 'Gear', text: 'Comfortable daily trainers. No need for race shoes.' },
      { icon: 'psychology', title: 'Mental Strategy', text: 'Run by feel — keep it conversational.' },
    ]
  },
  {
    id: 5,
    sessionNumber: 44,
    title: 'Strength & Core',
    difficulty: 'Moderate',
    duration: '45 min',
    intensity: 'Medium (5/10)',
    calories: '380 kcal',
    benefit: 'Strength',
    estimatedDistance: '—',
    heroImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuA2gYF38D3Da5qQvhLgVwD9WBLx7eCtiUtnfYCalwWEs_fmrRWmA0NhUlx2eGwAYJ1lSC7_e486ZLUf1CYPnVhMhPU60A3xRSMWa4SPVFSDDErTwW4NckFhFQ4TaQ_8gOzNkNgcVN6cn-OB51OjHDpdEH2-B9Xv0qpA1XIWFh3PTjsH1xyZujULhw3C9VDcchtTQxzPABAt1MK58JL8YrhKD0K71HoxuxRJMYErBZcCSlu7UsoexghUGvIDxKe0BbK_x4ua4ojQk7Q',
    steps: [
      { icon: 'fitness_center', title: 'Squats 3x15', subtitle: '10 min • Leg strength', pace: '—', highlight: true, muted: false },
      { icon: 'fitness_center', title: 'Lunges 3x12', subtitle: '8 min • Glutes & hamstrings', pace: '—', highlight: false, muted: false },
      { icon: 'fitness_center', title: 'Plank 3x60s', subtitle: '6 min • Core stability', pace: '—', highlight: false, muted: false },
      { icon: 'mode_cool_off', title: 'Mobility & Stretch', subtitle: '10 min • Full body', pace: '—', highlight: false, muted: false },
    ],
    prepTips: [
      { icon: 'restaurant', title: 'Pre-workout Fuel', text: 'Protein-rich snack 30–60 minutes before.' },
      { icon: 'water_drop', title: 'Hydration', text: 'Keep a water bottle nearby throughout.' },
      { icon: 'foot_bones', title: 'Gear', text: 'Flat training shoes for optimal stability during lifts.' },
      { icon: 'psychology', title: 'Mental Strategy', text: 'Focus on form over speed — control every rep.' },
    ]
  },
  {
    id: 6,
    sessionNumber: 45,
    title: 'Tempo Run',
    difficulty: 'Hard',
    duration: '65 min',
    intensity: 'High (7/10)',
    calories: '780 kcal',
    benefit: 'Lactate Threshold',
    estimatedDistance: '12.0 km',
    heroImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCWjKtZiwDas7wF8HbpQYepfrDwhAwL6nigLEx24wD6EUYb5b4OYA84oiV_uM1g28OOw9W6w5KN4Jberyo0O1LmJ3ecb8cxg1_DugFhqSfmfgTjC29aLtWwaazg3vH0RpTQ_9gOzNkNgcVN6cn-OB51OjHDpdEH2-B9Xv0qpA1XIWFh3PTjsH1xyZujULhw3C9VDcchtTQxzPABAt1MK58JL8YrhKD0K71HoxuxRJMYErBZcCSlu7UsoexghUGvIDxKe0BbK_x4ua4ojQk7Q',
    steps: [
      { icon: 'fitness_center', title: 'Warm-up', subtitle: '10 min • Easy Jog', pace: '06:00/km', highlight: false, muted: false },
      { icon: 'sprint', title: 'Tempo Block', subtitle: '40 min • Lactate Threshold • Zone 3–4', pace: '04:45/km', highlight: true, muted: false },
      { icon: 'mode_cool_off', title: 'Cool-down', subtitle: '15 min • Easy Jog + Walk', pace: '06:15/km', highlight: false, muted: false },
    ],
    prepTips: [
      { icon: 'restaurant', title: 'Pre-workout Fuel', text: 'Carb-heavy meal 2 hours before. Banana or gel 20 min before start.' },
      { icon: 'water_drop', title: 'Hydration', text: 'Electrolyte drink recommended — this is a sustained effort.' },
      { icon: 'foot_bones', title: 'Gear', text: 'Tempo shoes or race flats are ideal.' },
      { icon: 'psychology', title: 'Mental Strategy', text: 'Find your rhythm in the first 10 min and hold it steady.' },
    ]
  },
  {
    id: 7,
    sessionNumber: 46,
    title: 'Long Run',
    difficulty: 'Moderate',
    duration: '2h 10min',
    intensity: 'Medium (5/10)',
    calories: '1480 kcal',
    benefit: 'Endurance Base',
    estimatedDistance: '22.0 km',
    heroImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuB3EdaEeJ6PUfNqhhg8dBeJZPHHG2m1x7d7ZFyyLoW1KSueKICw4DBvU1VlKAtGqc19bd_fcjHcPwR9WlI2AfLhQA6-PX-qr0jq-6HHzOApdcxYiF3FnHdaw2JhlvXJw2YTQigsEB4etdv6o9rOaw5_R0Mr4cubnq-6TOFGTKqnjLtMA7Pqjij4gOMK9GgCdZjYodOWQZvDkJt8vLrLgB8cAO5AMsQuCcrT_YaZC72724eQ-kyf_VlRTaQ40CSoK8D13x8fx1gMJHs',
    steps: [
      { icon: 'fitness_center', title: 'Warm-up', subtitle: '10 min • Easy Walk/Jog', pace: '06:30/km', highlight: false, muted: false },
      { icon: 'directions_run', title: 'Long Easy Run', subtitle: '2 hrs • Zone 2 • Easy pace', pace: '05:55/km', highlight: true, muted: false },
      { icon: 'mode_cool_off', title: 'Walk & Stretch', subtitle: '10 min • Recovery', pace: '—', highlight: false, muted: false },
    ],
    prepTips: [
      { icon: 'restaurant', title: 'Pre-workout Fuel', text: 'Big carb-rich breakfast 2–3 hours before. Bring gels for miles 8+.' },
      { icon: 'water_drop', title: 'Hydration', text: 'Plan water stops every 5–6 km. Carry a soft flask.' },
      { icon: 'foot_bones', title: 'Gear', text: 'Your go-to daily trainers. Body-glide for long distances.' },
      { icon: 'psychology', title: 'Mental Strategy', text: 'Split mentally into thirds. The first third is easy, second is steady, third is survival mode.' },
    ]
  },
];

@Component({
  selector: 'app-training-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './training-detail.html',
  styleUrl: './training-detail.scss'
})
export class TrainingDetail implements OnInit {
  training: TrainingDetailData | null = null;

  constructor(private route: ActivatedRoute, private router: Router) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.training = TRAININGS.find(t => t.id === id) ?? null;
  }

  goBack(): void {
    this.router.navigate(['/training-plans']);
  }
}

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

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
export class Competitions {
  activeFilter = 'All Races';
  selectedRace: Race | null = null;
  selectedTargetTime = 'Sub 3:30';

  filters = ['All Races', 'Marathon', 'Half Marathon', '10K Run', '5K Sprint'];

  targetTimes = ['Sub 3:30', 'Sub 4:00', 'Sub 4:30', 'Just Finish'];

  races: Race[] = [
    {
      id: 1,
      tag: 'Major',
      tagClass: 'tag-major',
      date: 'Sept 24, 2024',
      name: 'Berlin Marathon',
      distance: '42.2 km',
      location: 'Berlin, DE',
      image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCY0ob1wpMOeP6zfuLA-xzjVtIckonf1GuCS_vGNUyyaacDNeAeBAcVGWSPihwW0E5Y3I6KnQudalx5St9WOeVKQHfpL83DsrKqMhdRgiOVftcCzliFs_aUf_DgHpwryCGJ1EVblEq5skgr3OmkcR-R97nHWGy45KTvb95LVqrOkqO4cWsk0cGDmvBeHubPRM9V99muuux592kQFfw-SIrSrUH5m2deE_S5-B2ZxdEedfsgOK5asUM5jpyNLohwm5uC0_61hsPN89o'
    },
    {
      id: 2,
      tag: 'City Run',
      tagClass: 'tag-city',
      date: 'Oct 12, 2024',
      name: 'Munich 10k',
      distance: '10.0 km',
      location: 'Munich, DE',
      image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAeGUUGllRX4y_Jje09V_m_xnoRUjjpbMdcEP5Ra4R7uhJkaHsDmVreDqcwBbLdWWLp6MorUC-5F2MVBleoQcuyEGO12w--fUN3MrAha3DpIYUuvGoMC0m3WEQ6XrhOoUhsin4xzv0HEqeQ-5FUnqUswmSk-zfj9D1cA12wL9eWiv98Mlwt0VJUfP9WvTrRRUTyaWrlCKsHV220-W46K2ekA8fmzqdD8p3patR_OoDa2sFdvug9lCL4vOQRP0sYfO3Sz2aKRas5aqk'
    }
  ];

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

  get filteredPlans(): TrainingPlan[] {
    return this.trainingPlans.filter(p => p.targetTime === this.selectedTargetTime);
  }

  setFilter(filter: string): void {
    this.activeFilter = filter;
  }

  selectRace(race: Race): void {
    this.selectedRace = race;
    this.selectedTargetTime = 'Sub 3:30';
  }

  deselectRace(): void {
    this.selectedRace = null;
  }

  setTargetTime(time: string): void {
    this.selectedTargetTime = time;
  }
}

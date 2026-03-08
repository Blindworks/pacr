import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

interface TrainingDay {
  id: number;
  dayShort: string;
  dayNum: number;
  title: string;
  subtitle: string;
  status: 'completed' | 'today' | 'upcoming' | 'rest';
  icon?: string;
}

interface Stat {
  label: string;
  value: string;
  unit: string;
}

interface Insight {
  title: string;
  text: string;
}

@Component({
  selector: 'app-training-plan',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './training-plan.html',
  styleUrl: './training-plan.scss'
})
export class TrainingPlan {
  constructor(private router: Router) {}

  planName = 'Half Marathon Prep';
  weekLabel = 'Week 6 of 12 • Sub-1:45 Goal';
  progressPercent = 45;
  completedSessions = 18;
  totalSessions = 40;

  currentWeekLabel = 'This Week';

  days: TrainingDay[] = [
    {
      id: 1,
      dayShort: 'Mon', dayNum: 12,
      title: 'Recovery Run',
      subtitle: '5.0 km • 32:40 • 6:32 /km',
      status: 'completed'
    },
    {
      id: 2,
      dayShort: 'Tue', dayNum: 13,
      title: 'Threshold Intervals',
      subtitle: '8.0 km total • 4x1km @ 4:20 pace',
      status: 'today'
    },
    {
      id: 3,
      dayShort: 'Wed', dayNum: 14,
      title: 'Rest Day',
      subtitle: 'Active mobility session recommended',
      status: 'rest',
      icon: 'hotel'
    },
    {
      id: 4,
      dayShort: 'Thu', dayNum: 15,
      title: 'Aerobic Base Run',
      subtitle: '10.0 km • Zone 2 effort',
      status: 'upcoming',
      icon: 'directions_run'
    },
    {
      id: 5,
      dayShort: 'Fri', dayNum: 16,
      title: 'Strength & Core',
      subtitle: '45 min • Gym session',
      status: 'upcoming',
      icon: 'fitness_center'
    },
    {
      id: 6,
      dayShort: 'Sat', dayNum: 17,
      title: 'Tempo Run',
      subtitle: '12.0 km • Lactate threshold',
      status: 'upcoming',
      icon: 'directions_run'
    },
    {
      id: 7,
      dayShort: 'Sun', dayNum: 18,
      title: 'Long Run',
      subtitle: '22.0 km • Easy pace',
      status: 'upcoming',
      icon: 'directions_run'
    }
  ];

  insights: Insight[] = [
    {
      title: 'Training Load: Optimal',
      text: 'Your recovery scores have been consistent. You\'re ready for today\'s high-intensity session.'
    },
    {
      title: 'Pace Trend',
      text: 'Threshold pace has improved by 4s/km over the last 14 days. Keep it up!'
    }
  ];

  stats: Stat[] = [
    { label: 'Weekly Distance', value: '24.2', unit: 'km' },
    { label: 'Time on Feet', value: '2:15', unit: 'h' },
    { label: 'Avg. HR', value: '142', unit: 'bpm' },
    { label: 'Elevation', value: '310', unit: 'm' }
  ];

  recoveryScore = 82;

  prevWeek(): void { /* navigate to previous week */ }
  nextWeek(): void { /* navigate to next week */ }
  startWorkout(): void { /* start today's workout */ }

  viewDetail(id: number): void {
    this.router.navigate(['/training-plans', id]);
  }
}

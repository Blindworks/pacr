import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';

export interface ActivityDetailData {
  id: number;
  name: string;
  date: string;
  label: string;
  distance: string;
  distanceRaw: number;
  time: string;
  pace: string;
  type: 'road' | 'trail' | 'track';
  mapImage: string;
  calories: number;
  elevGain: string;
  cadence: number;
  trainingEffect: string;
  trainingEffectLabel: string;
  insight: string;
}

const ACTIVITIES: ActivityDetailData[] = [
  {
    id: 1,
    name: 'Sunday Long Run',
    date: 'Oct 29, 2023 • 08:30 AM',
    label: 'Long Run',
    distance: '19.9',
    distanceRaw: 19.9,
    time: '1:42:05',
    pace: '5:08',
    type: 'road',
    mapImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBzhJYjjcQoTEoemxeKBVR33EvCaptA2wIPhERpIqDqbJbPBso3ccYP42UY4nbptfrYJa7cOBhfuMl-5vpF0Gg-9RpJyVEK4bfyrCNWlixF6uP3ExS7gWSRSfmHiMMWpqppyPvdxVZEgawJLwWb5_UiFq1nEMI_pUEYxnRnhy2YSx4NGoUUZhXJHxHNqKICFi-Vso1T1gxLrlG4qf5pYXZP5td4AZkGgzXhz7k2kaDBz_6a1u7A0Rqb4Czizk0a1pe9plcUYJNCtJk',
    calories: 1340,
    elevGain: '257 m',
    cadence: 174,
    trainingEffect: '4.6',
    trainingEffectLabel: 'Aerobic',
    insight: 'Your long run pace was consistent throughout, with only a 4% drop in the final 5 km. Excellent endurance base — consider adding 2 km to next week\'s long run to build towards your race goal.'
  },
  {
    id: 2,
    name: 'Trail Recovery',
    date: 'Oct 27, 2023 • 5:15 PM',
    label: 'Recovery',
    distance: '6.8',
    distanceRaw: 6.8,
    time: '40:12',
    pace: '5:54',
    type: 'trail',
    mapImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuB3EdaEeJ6PUfNqhhg8dBeJZPHHG2m1x7d7ZFyyLoW1KSueKICw4DBvU1VlKAtGqc19bd_fcjHcPwR9WlI2AfLhQA6-PX-qr0jq-6HHzOApdcxYiF3FnHdaw2JhlvXJw2YTQigsEB4etdv6o9rOaw5_R0Mr4cubnq-6TOFGTKqnjLtMA7Pqjij4gOMK9GgCdZjYodOWQZvDkJt8vLrLgB8cAO5AMsQuCcrT_YaZC72724eQ-kyf_VlRTaQ40CSoK8D13x8fx1gMJHs',
    calories: 420,
    elevGain: '88 m',
    cadence: 168,
    trainingEffect: '2.1',
    trainingEffectLabel: 'Aerobic',
    insight: 'Heart rate stayed in Zone 2 for 91% of this run — ideal recovery effort. Your body is adapting well. This is exactly the right intensity for a recovery day after a hard session.'
  },
  {
    id: 3,
    name: 'Interval Sprints',
    date: 'Oct 25, 2023 • 07:00 AM',
    label: 'Speed Work',
    distance: '5.0',
    distanceRaw: 5.0,
    time: '22:45',
    pace: '4:33',
    type: 'track',
    mapImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuA2gYF38D3Da5qQvhLgVwD9WBLx7eCtiUtnfYCalwWEs_fmrRWmA0NhUlx2eGwAYJ1lSC7_e486ZLUf1CYPnVhMhPU60A3xRSMWa4SPVFSDDErTwW4NckFhFQ4TaQ_8gOzNkNgcVN6cn-OB51OjHDpdEH2-B9Xv0qpA1XIWFh3PTjsH1xyZujULhw3C9VDcchtTQxzPABAt1MK58JL8YrhKD0K71HoxuxRJMYErBZcCSlu7UsoexghUGvIDxKe0BbK_x4ua4ojQk7Q',
    calories: 385,
    elevGain: '12 m',
    cadence: 186,
    trainingEffect: '4.9',
    trainingEffectLabel: 'Anaerobic',
    insight: 'Peak pace of 3:48/km during rep 4 — your fastest split this cycle. VO2max stimulus was high for all 5 intervals. Recovery time between reps was optimal. Speed is trending upward.'
  },
  {
    id: 4,
    name: 'Evening Commute',
    date: 'Oct 24, 2023 • 6:42 PM',
    label: 'Easy Run',
    distance: '9.3',
    distanceRaw: 9.3,
    time: '48:30',
    pace: '5:13',
    type: 'road',
    mapImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAhSLEmQhoX6CkmeCJeY1ujwgW-YQSZfky00vBXuFTugss8Oxv_BGtfj4u7tWQnQIasayoD_b6d5Gd7r64w9NQfmzE9oCkvZlxnERpnOlit2vDd8St1KdrxVf7i-Kypbr8rGtEMoegRuNvLTHP8PPAODOq1u5XhymtW3e8HTxrmNM22RPG0SbvmkgDMNvqw05GYYDbX_4pCrh0msot-p7U_m_ZDYHJDb0SQoEi3QITGZhFoTRrlb0-9PwVVuOcKWZT1O01gv3OpxXg',
    calories: 618,
    elevGain: '45 m',
    cadence: 176,
    trainingEffect: '3.1',
    trainingEffectLabel: 'Aerobic',
    insight: 'Consistent negative split — second half was 8 seconds/km faster than the first, indicating good pacing strategy. Cadence above 175 spm throughout shows efficient stride mechanics.'
  },
  {
    id: 5,
    name: 'Elevation Challenge',
    date: 'Oct 22, 2023 • 09:10 AM',
    label: 'Trail Run',
    distance: '10.5',
    distanceRaw: 10.5,
    time: '1:02:15',
    pace: '5:56',
    type: 'trail',
    mapImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCWjKtZiwDas7wF8HbpQYepfrDwhAwL6nigLEx24wD6EUYb5b4OYA84oiV_uM1g28OOw9W6w5KN4Jberyo0O1LmJ3ecb8cxg1_DugFhqSfmfgTjC29aLtWwaazg3vH0RpTQ_xUoPpN2_Qxebk6WhIZtPuzXIoXHOF99rTn3oaYuhXNbylJdblBAm10rKT5faysFPUBeXse8h5RQ471-72GJYneDfm8VnP_Z5TeQycOU2buKF53IIH5fYSdL3qw_XZEw8YZ-ykvJ0Zw',
    calories: 820,
    elevGain: '412 m',
    cadence: 164,
    trainingEffect: '4.3',
    trainingEffectLabel: 'Aerobic',
    insight: '412 m elevation gain — your highest climb this month. Grade-adjusted pace of 5:11/km puts this in line with your road tempo efforts, showing strong hill running adaptation.'
  },
];

@Component({
  selector: 'app-activity-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './activity-detail.html',
  styleUrl: './activity-detail.scss'
})
export class ActivityDetail implements OnInit {
  activity: ActivityDetailData | null = null;

  constructor(private route: ActivatedRoute, private router: Router) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.activity = ACTIVITIES.find(a => a.id === id) ?? null;
  }

  goBack(): void {
    this.router.navigate(['/activities']);
  }

  get hrPoints(): string {
    // Generate HR path across the run distance
    const points = [180, 155, 148, 152, 162, 158, 170, 165, 158, 145, 140];
    const w = 1000, h = 256;
    const coords = points.map((bpm, i) => {
      const x = (i / (points.length - 1)) * w;
      // Map 130–185 bpm to 220–20 px (inverted y)
      const y = h - ((bpm - 130) / 55) * (h - 20);
      return `${x} ${y}`;
    });
    return `M ${coords.join(' L ')}`;
  }

  get elevationPath(): string {
    const gains = [0, 10, 35, 60, 80, 95, 110, 115, 108, 90, 70];
    const w = 1000, h = 256;
    const maxGain = 120;
    const points = gains.map((g, i) => {
      const x = (i / (gains.length - 1)) * w;
      const y = h - (g / maxGain) * (h - 30) - 10;
      return `${x} ${y}`;
    });
    return `M 0 ${h} L ${points.join(' L ')} L ${w} ${h} Z`;
  }
}

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

interface Strategy {
  icon: string;
  name: string;
  description: string;
  sessions: string;
  active: boolean;
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

  filters = ['All Races', 'Marathon', 'Half Marathon', '10K Run', '5K Sprint'];

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

  strategies: Strategy[] = [
    {
      icon: 'child_care',
      name: 'Beginner',
      description: 'Focus on finishing and building endurance. 12-week program.',
      sessions: '3-4 Sessions/wk',
      active: false
    },
    {
      icon: 'directions_run',
      name: 'Intermediate',
      description: 'Improve speed and metabolic efficiency. 16-week program.',
      sessions: '5 Sessions/wk',
      active: false
    },
    {
      icon: 'bolt',
      name: 'Advanced',
      description: 'High mileage and intense interval work for serious athletes.',
      sessions: '6 Sessions/wk',
      active: true
    },
    {
      icon: 'trophy',
      name: 'PB-Chaser',
      description: 'Hyper-personalized plan targeting your specific time goal.',
      sessions: 'Custom',
      active: false
    }
  ];

  setFilter(filter: string): void {
    this.activeFilter = filter;
  }
}

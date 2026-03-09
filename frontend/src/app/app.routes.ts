import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./components/dashboard/dashboard').then(m => m.Dashboard)
  },
  {
    path: 'activities',
    loadComponent: () => import('./components/activities/activities').then(m => m.Activities)
  },
  {
    path: 'activities/:id',
    loadComponent: () => import('./components/activity-detail/activity-detail').then(m => m.ActivityDetail)
  },
  {
    path: 'training-plans',
    loadComponent: () => import('./components/training-plan/training-plan').then(m => m.TrainingPlan)
  },
  {
    path: 'training-plans/:id',
    loadComponent: () => import('./components/training-detail/training-detail').then(m => m.TrainingDetail)
  },
  {
    path: 'statistics',
    loadComponent: () => import('./components/statistics/statistics').then(m => m.Statistics)
  },
  {
    path: 'elite-upgrade',
    loadComponent: () => import('./components/elite-upgrade/elite-upgrade').then(m => m.EliteUpgrade)
  },
  {
    path: 'settings',
    loadComponent: () => import('./components/settings/settings').then(m => m.Settings)
  }
];

import { Routes } from '@angular/router';
import { AdminShell } from './admin-shell/admin-shell';

export const adminRoutes: Routes = [
  {
    path: '',
    component: AdminShell,
    children: [
      { path: '', redirectTo: 'plans', pathMatch: 'full' },
      {
        path: 'plans',
        loadComponent: () => import('./plans/plan-list/plan-list').then(m => m.PlanList)
      },
      {
        path: 'plans/new',
        loadComponent: () => import('./plans/plan-form/plan-form').then(m => m.PlanForm)
      },
      {
        path: 'plans/:planId/edit',
        loadComponent: () => import('./plans/plan-form/plan-form').then(m => m.PlanForm)
      },
      {
        path: 'plans/:planId/trainings',
        loadComponent: () => import('./trainings/training-list/training-list').then(m => m.TrainingList)
      },
      {
        path: 'plans/:planId/trainings/new',
        loadComponent: () => import('./trainings/training-form/training-form').then(m => m.TrainingForm)
      },
      {
        path: 'plans/:planId/trainings/:id/edit',
        loadComponent: () => import('./trainings/training-form/training-form').then(m => m.TrainingForm)
      }
    ]
  }
];

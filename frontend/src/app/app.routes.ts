import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./components/login/login').then(m => m.Login),
    data: { fullPage: true }
  },
  {
    path: 'signup',
    loadComponent: () => import('./components/signup/signup').then(m => m.Signup),
    data: { fullPage: true }
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./components/forgot-password/forgot-password').then(m => m.ForgotPassword),
    data: { fullPage: true }
  },
  {
    path: 'forgot-password/confirmation',
    loadComponent: () => import('./components/forgot-password-confirmation/forgot-password-confirmation').then(m => m.ForgotPasswordConfirmation),
    data: { fullPage: true }
  },
  {
    path: 'new-password',
    loadComponent: () => import('./components/new-password/new-password').then(m => m.NewPassword),
    data: { fullPage: true }
  },
  {
    path: '',
    pathMatch: 'full',
    canActivate: [authGuard],
    loadComponent: () => import('./components/dashboard/dashboard').then(m => m.Dashboard)
  },
  {
    path: 'activities',
    canActivate: [authGuard],
    loadComponent: () => import('./components/activities/activities').then(m => m.Activities)
  },
  {
    path: 'activities/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./components/activity-detail/activity-detail').then(m => m.ActivityDetail)
  },
  {
    path: 'training-plans',
    canActivate: [authGuard],
    loadComponent: () => import('./components/training-plan/training-plan').then(m => m.TrainingPlan)
  },
  {
    path: 'training-plans/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./components/training-detail/training-detail').then(m => m.TrainingDetail)
  },
  {
    path: 'statistics',
    canActivate: [authGuard],
    loadComponent: () => import('./components/statistics/statistics').then(m => m.Statistics)
  },
  {
    path: 'elite-upgrade',
    canActivate: [authGuard],
    loadComponent: () => import('./components/elite-upgrade/elite-upgrade').then(m => m.EliteUpgrade)
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () => import('./components/settings/settings').then(m => m.Settings)
  }
];

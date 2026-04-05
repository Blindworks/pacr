import { Routes } from '@angular/router';
import { AdminShell } from './admin-shell/admin-shell';

export const adminRoutes: Routes = [
  {
    path: '',
    component: AdminShell,
    children: [
      { path: '', redirectTo: 'overview', pathMatch: 'full' },
      {
        path: 'overview',
        loadComponent: () => import('./overview/admin-overview').then(m => m.AdminOverview)
      },
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
      },
      {
        path: 'competitions',
        loadComponent: () => import('./competitions/competition-list/competition-list').then(m => m.CompetitionList)
      },
      {
        path: 'competitions/new',
        loadComponent: () => import('./competitions/competition-form/competition-form').then(m => m.CompetitionForm)
      },
      {
        path: 'competitions/:compId/edit',
        loadComponent: () => import('./competitions/competition-form/competition-form').then(m => m.CompetitionForm)
      },
      {
        path: 'users',
        loadComponent: () => import('./users/user-list/user-list').then(m => m.UserList)
      },
      {
        path: 'users/:id/edit',
        loadComponent: () => import('./users/user-form/user-form').then(m => m.UserForm)
      },
      {
        path: 'achievements',
        loadComponent: () => import('./achievements/achievement-list/achievement-list').then(m => m.AchievementList)
      },
      {
        path: 'achievements/new',
        loadComponent: () => import('./achievements/achievement-form/achievement-form').then(m => m.AchievementForm)
      },
      {
        path: 'achievements/:id/edit',
        loadComponent: () => import('./achievements/achievement-form/achievement-form').then(m => m.AchievementForm)
      },
      {
        path: 'news',
        loadComponent: () => import('./news/news-list/news-list').then(m => m.NewsList)
      },
      {
        path: 'news/new',
        loadComponent: () => import('./news/news-form/news-form').then(m => m.NewsForm)
      },
      {
        path: 'news/:id/edit',
        loadComponent: () => import('./news/news-form/news-form').then(m => m.NewsForm)
      },
      {
        path: 'login-messages',
        loadComponent: () => import('./login-messages/login-message-list/login-message-list').then(m => m.LoginMessageList)
      },
      {
        path: 'login-messages/new',
        loadComponent: () => import('./login-messages/login-message-form/login-message-form').then(m => m.LoginMessageForm)
      },
      {
        path: 'login-messages/:id/edit',
        loadComponent: () => import('./login-messages/login-message-form/login-message-form').then(m => m.LoginMessageForm)
      },
      {
        path: 'feedback',
        loadComponent: () => import('./feedback/feedback-list/feedback-list').then(m => m.FeedbackList)
      }
    ]
  }
];

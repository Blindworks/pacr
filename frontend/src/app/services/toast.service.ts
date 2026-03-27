import { Injectable, signal } from '@angular/core';
import { Achievement } from './achievement.service';

export interface Toast {
  id: number;
  message: string;
  type: 'success' | 'info' | 'achievement';
  icon?: string;
  achievement?: Achievement;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private nextId = 0;

  show(message: string, type: 'success' | 'info' = 'info'): void {
    const toast: Toast = { id: this.nextId++, message, type };
    this.toasts.update(t => [...t, toast]);
    setTimeout(() => this.dismiss(toast.id), 5000);
  }

  showAchievement(achievement: Achievement): void {
    const toast: Toast = {
      id: this.nextId++,
      message: achievement.name,
      type: 'achievement',
      icon: achievement.icon,
      achievement
    };
    this.toasts.update(t => [...t, toast]);
    setTimeout(() => this.dismiss(toast.id), 6000);
  }

  dismiss(id: number): void {
    this.toasts.update(t => t.filter(toast => toast.id !== id));
  }
}

import { Injectable, inject, computed } from '@angular/core';
import { UserService } from './user.service';

const PRO_FEATURES = new Set([
  'statistics',
  'ai-trainer',
  'community-routes',
  'body-metrics',
  'cycle-tracking',
  'asthma-tracking',
  'achievements',
  'insights',
  'activity-detail-advanced',
]);

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private readonly userService = inject(UserService);

  readonly isPro = computed(() => {
    const user = this.userService.currentUser();
    if (!user || user.subscriptionPlan !== 'PRO') return false;
    if (!user.subscriptionExpiresAt) return true; // no expiry = lifetime
    return new Date(user.subscriptionExpiresAt) > new Date();
  });

  readonly isAdmin = computed(() => {
    return this.userService.currentUser()?.role === 'ADMIN';
  });

  canAccess(featureKey: string): boolean {
    if (!PRO_FEATURES.has(featureKey)) return true;
    return this.isPro() || this.isAdmin();
  }
}

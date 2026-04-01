import { Component, computed, inject, input } from '@angular/core';
import { Router } from '@angular/router';
import { SubscriptionService } from '../../../services/subscription.service';

@Component({
  selector: 'app-pro-overlay',
  standalone: true,
  templateUrl: './pro-overlay.html',
  styleUrl: './pro-overlay.scss'
})
export class ProOverlay {
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly router = inject(Router);

  featureKey = input.required<string>();

  readonly locked = computed(() => !this.subscriptionService.canAccess(this.featureKey()));

  navigateToUpgrade(): void {
    this.router.navigate(['/elite-upgrade']);
  }
}

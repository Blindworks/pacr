import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserService } from '../services/user.service';
import { firstValueFrom } from 'rxjs';

export const authGuard: CanActivateFn = async (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const userService = inject(UserService);

  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/login']);
  }

  // Load user if not yet loaded
  let user = userService.currentUser();
  if (!user) {
    try {
      user = await firstValueFrom(userService.getMe());
    } catch {
      return router.createUrlTree(['/login']);
    }
  }

  // Check onboarding status — don't redirect if already on /onboarding
  const targetPath = route.routeConfig?.path;
  if (!user.onboardingCompleted && targetPath !== 'onboarding') {
    return router.createUrlTree(['/onboarding']);
  }

  return true;
};

import { Component, signal, inject, effect } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { Sidebar } from './components/sidebar/sidebar';
import { PaceCalculatorDialog } from './components/pace-calculator/pace-calculator-dialog';
import { AboutDialog } from './components/about-dialog/about-dialog';
import { Toast } from './components/toast/toast';
import { FeedbackFab } from './components/feedback-fab/feedback-fab';
import { FeedbackDialog } from './components/feedback-dialog/feedback-dialog';
import { filter, map, startWith } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { UserService } from './services/user.service';
import { ThemeService } from './services/theme.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Sidebar, PaceCalculatorDialog, AboutDialog, Toast, FeedbackFab, FeedbackDialog],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('frontend');

  private readonly router = inject(Router);
  private readonly userService = inject(UserService);
  private readonly themeService = inject(ThemeService);

  constructor() {
    effect(() => {
      const user = this.userService.currentUser();
      if (user) {
        this.themeService.initFromProfile(user.theme);
      }
    });
  }

  private readonly hideSidebarPaths = ['/login', '/signup', '/forgot-password', '/new-password', '/onboarding'];

  private shouldShowSidebar(url: string): boolean {
    return url !== '/' && !this.hideSidebarPaths.some(p => url.startsWith(p));
  }

  protected readonly showSidebar = toSignal(
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      map((e: NavigationEnd) => this.shouldShowSidebar(e.urlAfterRedirects)),
      startWith(this.shouldShowSidebar(this.router.url))
    )
  );
}

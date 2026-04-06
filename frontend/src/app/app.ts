import { Component, signal, inject, effect, ViewChild } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { NgClass } from '@angular/common';
import { Sidebar } from './components/sidebar/sidebar';
import { PaceCalculatorDialog } from './components/pace-calculator/pace-calculator-dialog';
import { AboutDialog } from './components/about-dialog/about-dialog';
import { Toast } from './components/toast/toast';
import { FeedbackFab } from './components/feedback-fab/feedback-fab';
import { FeedbackDialog } from './components/feedback-dialog/feedback-dialog';
import { LoginMessageDialog } from './components/login-message-dialog/login-message-dialog';
import { filter, map, startWith } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { UserService } from './services/user.service';
import { ThemeService } from './services/theme.service';
import { LoginMessageService } from './services/login-message.service';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NgClass, Sidebar, PaceCalculatorDialog, AboutDialog, Toast, FeedbackFab, FeedbackDialog, LoginMessageDialog],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('frontend');
  protected readonly sidebarCollapsed = signal(false);
  protected readonly isMobile = signal(false);

  @ViewChild(Sidebar) private sidebarComponent?: Sidebar;

  private readonly router = inject(Router);
  private readonly userService = inject(UserService);
  private readonly themeService = inject(ThemeService);
  private readonly loginMessageService = inject(LoginMessageService);
  private readonly translate = inject(TranslateService);

  private mobileQuery!: MediaQueryList;
  private mobileListener = (e: MediaQueryListEvent) => this.isMobile.set(e.matches);

  constructor() {
    this.translate.use(localStorage.getItem('pacr-language') || 'de');

    effect(() => {
      const user = this.userService.currentUser();
      if (user) {
        this.themeService.initFromProfile(user.theme);
        this.loginMessageService.fetchPending();
      }
    });

    this.mobileQuery = window.matchMedia('(max-width: 767px)');
    this.isMobile.set(this.mobileQuery.matches);
    this.mobileQuery.addEventListener('change', this.mobileListener);
  }

  private readonly hideSidebarPaths = ['/login', '/signup', '/forgot-password', '/new-password', '/verify-email', '/onboarding'];

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

  onSidebarCollapsed(collapsed: boolean): void {
    this.sidebarCollapsed.set(collapsed);
  }

  openMobileSidebar(): void {
    this.sidebarComponent?.toggleMobile();
  }
}

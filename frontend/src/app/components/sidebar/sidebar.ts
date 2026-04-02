import { Component, OnInit, OnDestroy, inject, signal, computed, output, HostListener } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { NgClass } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { PaceCalculatorService } from '../../services/pace-calculator.service';
import { AboutDialogService } from '../../services/about-dialog.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { ThemeService } from '../../services/theme.service';
import { SubscriptionService } from '../../services/subscription.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgClass, TranslateModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class Sidebar implements OnInit, OnDestroy {
  bodyDataOpen = false;
  profileImageUrl = signal<string | null>(null);

  /** Sidebar collapsed (icon-only) — for tablet/desktop toggle */
  collapsed = signal(false);

  /** Sidebar open as overlay on mobile */
  mobileOpen = signal(false);

  /** Whether we're in mobile breakpoint (<768px) */
  isMobile = signal(false);

  /** Emits collapsed state so parent can adjust layout */
  collapsedChange = output<boolean>();

  private readonly paceCalc = inject(PaceCalculatorService);
  private readonly aboutDialog = inject(AboutDialogService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  protected readonly userService = inject(UserService);
  protected readonly themeService = inject(ThemeService);
  protected readonly subscriptionService = inject(SubscriptionService);

  private mobileQuery!: MediaQueryList;
  private tabletQuery!: MediaQueryList;
  private mobileListener = (e: MediaQueryListEvent) => this.onMobileChange(e.matches);
  private tabletListener = (e: MediaQueryListEvent) => this.onTabletChange(e.matches);

  readonly isAdmin = computed(() => {
    const role = this.userService.currentUser()?.role ?? this.authService.getRole();
    return role === 'ADMIN';
  });

  readonly isPro = computed(() => this.subscriptionService.isPro() || this.subscriptionService.isAdmin());

  readonly membershipLabel = computed(() => {
    const role = this.userService.currentUser()?.role;
    if (role === 'ADMIN') return this.translate.instant('SIDEBAR.ADMIN');
    if (this.subscriptionService.isPro()) return this.translate.instant('SIDEBAR.PRO_MEMBER');
    return this.translate.instant('SIDEBAR.MEMBER');
  });

  ngOnInit(): void {
    // Profile image loading
    if (!this.userService.currentUser()) {
      this.userService.getMe().subscribe({
        next: user => this.loadProfileImage(user.id),
        error: () => {}
      });
    } else {
      const user = this.userService.currentUser();
      if (user) this.loadProfileImage(user.id);
    }

    // Media query breakpoints
    this.mobileQuery = window.matchMedia('(max-width: 767px)');
    this.tabletQuery = window.matchMedia('(min-width: 768px) and (max-width: 900px)');

    this.onMobileChange(this.mobileQuery.matches);
    if (!this.mobileQuery.matches) {
      this.onTabletChange(this.tabletQuery.matches);
    }

    this.mobileQuery.addEventListener('change', this.mobileListener);
    this.tabletQuery.addEventListener('change', this.tabletListener);

    // Close mobile sidebar on navigation
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe(() => {
      if (this.isMobile()) {
        this.mobileOpen.set(false);
      }
    });
  }

  ngOnDestroy(): void {
    const url = this.profileImageUrl();
    if (url) URL.revokeObjectURL(url);

    this.mobileQuery?.removeEventListener('change', this.mobileListener);
    this.tabletQuery?.removeEventListener('change', this.tabletListener);
  }

  private onMobileChange(matches: boolean): void {
    this.isMobile.set(matches);
    if (matches) {
      this.collapsed.set(false);
      this.mobileOpen.set(false);
    }
  }

  private onTabletChange(matches: boolean): void {
    if (!this.isMobile()) {
      this.collapsed.set(matches);
      this.collapsedChange.emit(matches);
    }
  }

  toggleCollapse(): void {
    if (this.isMobile()) {
      this.mobileOpen.set(false);
      return;
    }
    const next = !this.collapsed();
    this.collapsed.set(next);
    this.collapsedChange.emit(next);
  }

  toggleMobile(): void {
    this.mobileOpen.set(!this.mobileOpen());
  }

  closeMobile(): void {
    this.mobileOpen.set(false);
  }

  private loadProfileImage(userId: number): void {
    this.userService.getProfileImage(userId).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const old = this.profileImageUrl();
        if (old) URL.revokeObjectURL(old);
        this.profileImageUrl.set(url);
      },
      error: () => {}
    });
  }

  toggleBodyData(): void {
    this.bodyDataOpen = !this.bodyDataOpen;
  }

  openPaceCalculator(): void {
    this.paceCalc.open();
  }

  openAbout(): void {
    this.aboutDialog.open();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}

import { Injectable, inject, signal, effect, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type ThemeChoice = 'light' | 'dark' | 'auto';
export type ResolvedTheme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly platformId = inject(PLATFORM_ID);

  readonly choice = signal<ThemeChoice>('dark');
  readonly resolved = signal<ResolvedTheme>('dark');

  private mediaQuery: MediaQueryList | null = null;

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.mediaQuery = window.matchMedia('(prefers-color-scheme: light)');
      this.mediaQuery.addEventListener('change', () => this.resolve());
    }

    effect(() => {
      this.choice();
      this.resolve();
    });

    effect(() => {
      const theme = this.resolved();
      if (isPlatformBrowser(this.platformId)) {
        document.body.setAttribute('data-theme', theme);
      }
    });
  }

  initFromProfile(theme: string | null | undefined): void {
    const t = (theme === 'light' || theme === 'dark' || theme === 'auto') ? theme : 'dark';
    this.choice.set(t);
  }

  setTheme(value: ThemeChoice): void {
    this.choice.set(value);
  }

  private resolve(): void {
    const c = this.choice();
    if (c === 'auto') {
      this.resolved.set(this.mediaQuery?.matches ? 'light' : 'dark');
    } else {
      this.resolved.set(c);
    }
  }
}

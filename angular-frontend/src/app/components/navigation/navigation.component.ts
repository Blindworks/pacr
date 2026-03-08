import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { I18nService } from '../../services/i18n.service';
import { Language } from '../../i18n/translations';
import { PaceConverterDialogComponent } from '../pace-converter-dialog/pace-converter-dialog.component';

@Component({
  selector: 'app-navigation',
  imports: [
    RouterModule,
    MatIconModule,
    MatButtonModule,
    TranslatePipe
  ],
  templateUrl: './navigation.component.html',
  styleUrl: './navigation.component.scss'
})
export class NavigationComponent implements OnInit, OnDestroy {
  mobileMenuOpen = false;
  readonly languages: readonly Language[];
  isFemale = false;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private apiService: ApiService,
    private i18nService: I18nService,
    private dialog: MatDialog
  ) {
    this.languages = this.i18nService.getSupportedLanguages();
    this.authService.authState$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadCurrentUserGender());
  }

  ngOnInit(): void {
    this.loadCurrentUserGender();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('window:resize')
  onResize(): void {
    if (window.innerWidth > 1024 && this.mobileMenuOpen) {
      this.mobileMenuOpen = false;
    }
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen = false;
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  getUsername(): string {
    return this.authService.getCurrentUsername() ?? this.i18nService.t('common.profile');
  }

  getInitial(): string {
    const name = this.authService.getCurrentUsername();
    return name ? name.charAt(0).toUpperCase() : 'P';
  }

  logout(): void {
    this.closeMobileMenu();
    this.authService.logout();
  }

  getLanguage(): Language {
    return this.i18nService.getLanguage();
  }

  onLanguageChange(language: string): void {
    this.i18nService.setLanguage(language);
  }

  toggleLanguage(): void {
    const current = this.i18nService.getLanguage();
    const next = this.languages.find(l => l !== current) ?? this.languages[0];
    this.i18nService.setLanguage(next);
  }

  openPaceConverterDialog(): void {
    this.closeMobileMenu();
    this.dialog.open(PaceConverterDialogComponent, {
      width: '800px',
      maxWidth: '95vw',
      maxHeight: '92vh'
    });
  }

  private loadCurrentUserGender(): void {
    if (!this.isLoggedIn()) {
      this.isFemale = false;
      return;
    }

    this.apiService.getMe()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: user => {
          this.isFemale = user.gender === 'FEMALE';
        },
        error: () => {
          this.isFemale = false;
        }
      });
  }
}

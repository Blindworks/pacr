import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, signal, inject, ViewChild, ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';
import { StravaService } from '../../services/strava.service';
import { CorosService } from '../../services/coros.service';
import { UserService, UserProfile } from '../../services/user.service';
import { NotificationPreferencesService } from '../../services/notification-preferences.service';
import { ThemeService, ThemeChoice } from '../../services/theme.service';

type Integration = {
  id: string;
  name: string;
  description: string;
  iconBg: string;
  connected: boolean;
};

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './settings.html',
  styleUrl: './settings.scss'
})
export class Settings implements OnInit, OnDestroy {
  @ViewChild('fileInput') private fileInput!: ElementRef<HTMLInputElement>;

  private readonly router = inject(Router);
  private readonly stravaService = inject(StravaService);
  private readonly corosService = inject(CorosService);
  private readonly userService = inject(UserService);
  private readonly notifPrefsService = inject(NotificationPreferencesService);
  private readonly themeService = inject(ThemeService);
  private readonly translate = inject(TranslateService);

  private readonly autoSave$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();
  private profileLoaded = false;

  private userId = 0;
  private currentUser: UserProfile | null = null;
  private profileImageObjectUrl: string | null = null;

  protected firstName = signal('');
  protected lastName = signal('');

  protected weight = signal('');
  protected height = signal('');
  protected dateOfBirth = signal('');
  protected gender = signal('Male');
  protected restingHr = signal('');
  protected maxHr = signal('');

  protected currentPassword = signal('');
  protected newPassword = signal('');
  protected confirmPassword = signal('');
  protected passwordError = signal('');
  protected passwordSuccess = signal('');
  protected passwordSaving = signal(false);

  protected unit = signal<'metric' | 'imperial'>('metric');
  protected theme = signal<'light' | 'dark' | 'auto'>('dark');
  protected currentLang = signal<string>(localStorage.getItem('pacr-language') || 'de');

  protected emailReminderEnabled = signal(false);
  protected emailReminderTime = signal('18:00');
  protected emailNewsEnabled = signal(false);

  protected profileImageUrl = signal<string | null>(null);
  protected imageUploading = signal(false);
  protected imageError = signal('');

  protected dwdRegionId = signal<number | null>(null);
  protected asthmaTrackingEnabled = signal(false);
  protected cycleTrackingEnabled = signal(false);
  protected communityRoutesEnabled = signal(false);

  protected stravaLoading = signal(false);
  protected corosLoading = signal(false);
  protected saving = signal(false);
  protected saveError = signal('');

  protected readonly reminderTimes = [
    '06:00', '07:00', '08:00', '09:00', '10:00', '11:00',
    '12:00', '13:00', '14:00', '15:00', '16:00', '17:00',
    '18:00', '19:00', '20:00', '21:00', '22:00'
  ];

  protected readonly dwdRegions = [
    { id: 10,  name: 'Schleswig-Holstein und Hamburg' },
    { id: 20,  name: 'Mecklenburg-Vorpommern' },
    { id: 30,  name: 'Niedersachsen und Bremen' },
    { id: 40,  name: 'Nordrhein-Westfalen' },
    { id: 50,  name: 'Brandenburg und Berlin' },
    { id: 60,  name: 'Sachsen-Anhalt' },
    { id: 70,  name: 'Thüringen' },
    { id: 80,  name: 'Sachsen' },
    { id: 90,  name: 'Bayern – Alpen' },
    { id: 91,  name: 'Bayern – Süd (ohne Alpen)' },
    { id: 92,  name: 'Bayern – Nord' },
    { id: 100, name: 'Baden-Württemberg' },
    { id: 110, name: 'Rheinland-Pfalz und Saarland' },
    { id: 120, name: 'Hessen' },
  ] as const;

  protected readonly integrations: Integration[] = [
    {
      id: 'strava',
      name: 'Strava',
      description: this.translate.instant('SETTINGS.STRAVA_DESC'),
      iconBg: '#FC6100',
      connected: false
    },
    {
      id: 'coros',
      name: 'COROS',
      description: this.translate.instant('SETTINGS.COROS_DESC'),
      iconBg: '#e7262a',
      connected: false
    },
    {
      id: 'garmin',
      name: 'Garmin Connect',
      description: this.translate.instant('SETTINGS.GARMIN_DESC'),
      iconBg: '#0076C0',
      connected: false
    },
    {
      id: 'apple',
      name: 'Apple Health',
      description: this.translate.instant('SETTINGS.APPLE_DESC'),
      iconBg: '#1c1c1e',
      connected: false
    }
  ];

  ngOnInit(): void {
    this.loadStravaStatus();
    this.loadCorosStatus();
    this.loadUserProfile();
    this.loadNotificationPreferences();

    this.autoSave$.pipe(
      debounceTime(800),
      takeUntil(this.destroy$)
    ).subscribe(() => this.saveChanges());
  }

  private triggerAutoSave(): void {
    if (this.profileLoaded) {
      this.autoSave$.next();
    }
  }

  private loadUserProfile(): void {
    this.userService.getMe().subscribe({
      next: user => {
        this.currentUser = user;
        this.userId = user.id;
        this.firstName.set(user.firstName ?? '');
        this.lastName.set(user.lastName ?? '');
        this.weight.set(user.weightKg != null ? String(user.weightKg) : '');
        this.height.set(user.heightCm != null ? String(user.heightCm) : '');
        this.gender.set(user.gender ?? 'Male');
        this.restingHr.set(user.hrRest != null ? String(user.hrRest) : '');
        this.maxHr.set(user.maxHeartRate != null ? String(user.maxHeartRate) : '');
        this.dateOfBirth.set(user.dateOfBirth ?? '');
        this.dwdRegionId.set(user.dwdRegionId ?? null);
        this.asthmaTrackingEnabled.set(user.asthmaTrackingEnabled ?? false);
        this.cycleTrackingEnabled.set(user.cycleTrackingEnabled ?? false);
        this.communityRoutesEnabled.set(user.communityRoutesEnabled ?? false);
        this.theme.set((user.theme as ThemeChoice) ?? 'dark');
        this.themeService.initFromProfile(user.theme);
        this.profileLoaded = true;
        this.loadProfileImage();
      },
      error: () => { /* keep defaults if backend unreachable */ }
    });
  }

  private loadNotificationPreferences(): void {
    this.notifPrefsService.getPreferences().subscribe({
      next: prefs => {
        this.emailReminderEnabled.set(prefs.emailReminderEnabled);
        this.emailReminderTime.set(prefs.emailReminderTime);
        this.emailNewsEnabled.set(prefs.emailNewsEnabled);
      },
      error: () => { /* keep defaults */ }
    });
  }

  private loadProfileImage(): void {
    if (!this.userId) return;
    this.userService.getProfileImage(this.userId).subscribe({
      next: blob => {
        if (this.profileImageObjectUrl) {
          URL.revokeObjectURL(this.profileImageObjectUrl);
        }
        this.profileImageObjectUrl = URL.createObjectURL(blob);
        this.profileImageUrl.set(this.profileImageObjectUrl);
      },
      error: () => { /* no image yet, keep placeholder */ }
    });
  }

  protected triggerFileInput(): void {
    this.fileInput.nativeElement.click();
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.userId) return;

    this.imageUploading.set(true);
    this.imageError.set('');

    this.userService.uploadProfileImage(this.userId, file).subscribe({
      next: () => {
        this.imageUploading.set(false);
        this.loadProfileImage();
      },
      error: () => {
        this.imageUploading.set(false);
        this.imageError.set(this.translate.instant('SETTINGS.UPLOAD_FAILED'));
      }
    });

    input.value = '';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.profileImageObjectUrl) {
      URL.revokeObjectURL(this.profileImageObjectUrl);
    }
  }

  private loadStravaStatus(): void {
    this.stravaService.getStatus().subscribe({
      next: status => {
        const strava = this.integrations.find(i => i.id === 'strava');
        if (strava) strava.connected = status.connected;
      },
      error: () => { /* backend unreachable, keep default */ }
    });
  }

  protected onAsthmaTrackingChange(value: boolean): void {
    this.asthmaTrackingEnabled.set(value);
    this.userService.currentUser.update(u => u ? { ...u, asthmaTrackingEnabled: value } : null);
    this.triggerAutoSave();
  }

  protected onCycleTrackingChange(value: boolean): void {
    this.cycleTrackingEnabled.set(value);
    this.userService.currentUser.update(u => u ? { ...u, cycleTrackingEnabled: value } : null);
    this.triggerAutoSave();
  }

  protected onCommunityRoutesChange(value: boolean): void {
    this.communityRoutesEnabled.set(value);
    this.userService.currentUser.update(u => u ? { ...u, communityRoutesEnabled: value } : null);
    this.triggerAutoSave();
  }

  protected setUnit(value: 'metric' | 'imperial'): void {
    this.unit.set(value);
    this.triggerAutoSave();
  }

  protected setTheme(value: 'light' | 'dark' | 'auto'): void {
    this.theme.set(value);
    this.themeService.setTheme(value);
    this.triggerAutoSave();
  }

  protected setLanguage(lang: string): void {
    this.translate.use(lang);
    localStorage.setItem('pacr-language', lang);
    this.currentLang.set(lang);
  }

  protected onFieldChange(): void {
    this.triggerAutoSave();
  }

  protected toggleIntegration(integration: Integration): void {
    if (integration.id === 'strava') {
      integration.connected ? this.disconnectStrava(integration) : this.connectStrava();
    } else if (integration.id === 'coros') {
      integration.connected ? this.disconnectCoros(integration) : this.connectCoros();
    }
  }

  private connectStrava(): void {
    this.stravaLoading.set(true);
    this.stravaService.getAuthUrl().subscribe({
      next: ({ url }) => window.location.href = url,
      error: () => this.stravaLoading.set(false)
    });
  }

  private disconnectStrava(integration: Integration): void {
    this.stravaService.disconnect().subscribe({
      next: () => { integration.connected = false; },
      error: () => { /* ignore */ }
    });
  }

  private loadCorosStatus(): void {
    this.corosService.getStatus().subscribe({
      next: status => {
        const coros = this.integrations.find(i => i.id === 'coros');
        if (coros) coros.connected = status.connected;
      },
      error: () => { /* backend unreachable, keep default */ }
    });
  }

  private connectCoros(): void {
    this.corosLoading.set(true);
    this.corosService.getAuthUrl().subscribe({
      next: ({ url }) => window.location.href = url,
      error: () => this.corosLoading.set(false)
    });
  }

  private disconnectCoros(integration: Integration): void {
    this.corosService.disconnect().subscribe({
      next: () => { integration.connected = false; },
      error: () => { /* ignore */ }
    });
  }

  protected saveChanges(): void {
    if (!this.userId || !this.currentUser) return;

    this.saving.set(true);
    this.saveError.set('');

    this.userService.updateUser(this.userId, {
      username: this.currentUser.username,
      email: this.currentUser.email,
      firstName: this.firstName(),
      lastName: this.lastName(),
      dateOfBirth: this.dateOfBirth() || null,
      heightCm: this.height() ? parseInt(this.height(), 10) : null,
      weightKg: this.weight() ? parseFloat(this.weight()) : null,
      maxHeartRate: this.maxHr() ? parseInt(this.maxHr(), 10) : null,
      hrRest: this.restingHr() ? parseInt(this.restingHr(), 10) : null,
      gender: this.gender(),
      status: this.currentUser.status,
      dwdRegionId: this.dwdRegionId(),
      asthmaTrackingEnabled: this.asthmaTrackingEnabled(),
      cycleTrackingEnabled: this.cycleTrackingEnabled(),
      communityRoutesEnabled: this.communityRoutesEnabled(),
      theme: this.theme()
    }).subscribe({
      next: updated => {
        this.currentUser = updated;
        this.saving.set(false);
        this.saveNotificationPreferences();
      },
      error: () => {
        this.saveError.set(this.translate.instant('SETTINGS.SAVE_FAILED'));
        this.saving.set(false);
      }
    });
  }

  protected changePassword(): void {
    this.passwordError.set('');
    this.passwordSuccess.set('');

    if (!this.currentPassword() || !this.newPassword() || !this.confirmPassword()) {
      this.passwordError.set(this.translate.instant('SETTINGS.PASSWORD_FILL_ALL'));
      return;
    }
    if (this.newPassword().length < 8) {
      this.passwordError.set(this.translate.instant('SETTINGS.PASSWORD_MIN_LENGTH'));
      return;
    }
    if (this.newPassword() !== this.confirmPassword()) {
      this.passwordError.set(this.translate.instant('SETTINGS.PASSWORD_NO_MATCH'));
      return;
    }

    this.passwordSaving.set(true);
    this.userService.changePassword(this.currentPassword(), this.newPassword()).subscribe({
      next: () => {
        this.passwordSaving.set(false);
        this.passwordSuccess.set(this.translate.instant('SETTINGS.PASSWORD_SUCCESS'));
        this.currentPassword.set('');
        this.newPassword.set('');
        this.confirmPassword.set('');
      },
      error: (err) => {
        this.passwordSaving.set(false);
        const msg = err.error?.message ?? this.translate.instant('SETTINGS.PASSWORD_ERROR');
        this.passwordError.set(msg);
      }
    });
  }

  protected restartOnboarding(): void {
    this.router.navigate(['/onboarding']);
  }

  private saveNotificationPreferences(): void {
    this.notifPrefsService.updatePreferences({
      emailReminderEnabled: this.emailReminderEnabled(),
      emailReminderTime: this.emailReminderTime(),
      emailNewsEnabled: this.emailNewsEnabled()
    }).subscribe({ error: () => { /* ignore, non-critical */ } });
  }
}

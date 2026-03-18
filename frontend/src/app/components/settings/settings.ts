import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, signal, inject, ViewChild, ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { StravaService } from '../../services/strava.service';
import { UserService, UserProfile } from '../../services/user.service';

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
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.html',
  styleUrl: './settings.scss'
})
export class Settings implements OnInit, OnDestroy {
  @ViewChild('fileInput') private fileInput!: ElementRef<HTMLInputElement>;

  private readonly stravaService = inject(StravaService);
  private readonly userService = inject(UserService);

  private userId = 0;
  private currentUser: UserProfile | null = null;
  private profileImageObjectUrl: string | null = null;

  protected weight = signal('');
  protected height = signal('');
  protected dateOfBirth = signal('');
  protected gender = signal('Male');
  protected restingHr = signal('');
  protected maxHr = signal('');

  protected unit = signal<'metric' | 'imperial'>('metric');
  protected theme = signal<'light' | 'dark' | 'auto'>('dark');
  protected pushNotifications = signal(true);
  protected emailDigest = signal(false);

  protected profileImageUrl = signal<string | null>(null);
  protected imageUploading = signal(false);
  protected imageError = signal('');

  protected dwdRegionId = signal<number | null>(null);
  protected asthmaTrackingEnabled = signal(false);

  protected stravaLoading = signal(false);
  protected saving = signal(false);
  protected saveError = signal('');

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
      description: 'Sync activities and segments automatically',
      iconBg: '#FC6100',
      connected: false
    },
    {
      id: 'garmin',
      name: 'Garmin Connect',
      description: 'Import health metrics and training data',
      iconBg: '#0076C0',
      connected: false
    },
    {
      id: 'apple',
      name: 'Apple Health',
      description: 'Sync steps, sleep, and heart rate',
      iconBg: '#1c1c1e',
      connected: false
    }
  ];

  ngOnInit(): void {
    this.loadStravaStatus();
    this.loadUserProfile();
  }

  private loadUserProfile(): void {
    this.userService.getMe().subscribe({
      next: user => {
        this.currentUser = user;
        this.userId = user.id;
        this.weight.set(user.weightKg != null ? String(user.weightKg) : '');
        this.height.set(user.heightCm != null ? String(user.heightCm) : '');
        this.gender.set(user.gender ?? 'Male');
        this.restingHr.set(user.hrRest != null ? String(user.hrRest) : '');
        this.maxHr.set(user.maxHeartRate != null ? String(user.maxHeartRate) : '');
        this.dateOfBirth.set(user.dateOfBirth ?? '');
        this.dwdRegionId.set(user.dwdRegionId ?? null);
        this.asthmaTrackingEnabled.set(user.asthmaTrackingEnabled ?? false);
        this.loadProfileImage();
      },
      error: () => { /* keep defaults if backend unreachable */ }
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
        this.imageError.set('Upload failed. Please try again.');
      }
    });

    input.value = '';
  }

  ngOnDestroy(): void {
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

  protected setUnit(value: 'metric' | 'imperial'): void {
    this.unit.set(value);
  }

  protected setTheme(value: 'light' | 'dark' | 'auto'): void {
    this.theme.set(value);
  }

  protected toggleIntegration(integration: Integration): void {
    if (integration.id === 'strava') {
      integration.connected ? this.disconnectStrava(integration) : this.connectStrava();
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

  protected saveChanges(): void {
    if (!this.userId || !this.currentUser) return;

    this.saving.set(true);
    this.saveError.set('');

    this.userService.updateUser(this.userId, {
      username: this.currentUser.username,
      email: this.currentUser.email,
      firstName: this.currentUser.firstName,
      lastName: this.currentUser.lastName,
      dateOfBirth: this.dateOfBirth() || null,
      heightCm: this.height() ? parseInt(this.height(), 10) : null,
      weightKg: this.weight() ? parseFloat(this.weight()) : null,
      maxHeartRate: this.maxHr() ? parseInt(this.maxHr(), 10) : null,
      hrRest: this.restingHr() ? parseInt(this.restingHr(), 10) : null,
      gender: this.gender(),
      status: this.currentUser.status,
      dwdRegionId: this.dwdRegionId(),
      asthmaTrackingEnabled: this.asthmaTrackingEnabled()
    }).subscribe({
      next: updated => {
        this.currentUser = updated;
        this.saving.set(false);
      },
      error: () => {
        this.saveError.set('Failed to save. Please try again.');
        this.saving.set(false);
      }
    });
  }
}

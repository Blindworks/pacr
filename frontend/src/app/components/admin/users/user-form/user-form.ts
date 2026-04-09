import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { UserService, UserProfile } from '../../../../services/user.service';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule, DatePipe],
  templateUrl: './user-form.html',
  styleUrl: './user-form.scss'
})
export class UserForm implements OnInit {
  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private translate = inject(TranslateService);

  userId = signal<number | null>(null);
  isLoading = signal(false);
  isSaving = signal(false);
  hasError = signal(false);
  loaded = signal<UserProfile | null>(null);

  get userStatuses() {
    return [
      { value: 'ACTIVE', label: this.translate.instant('ADMIN.STATUS_ACTIVE') },
      { value: 'INACTIVE', label: this.translate.instant('ADMIN.STATUS_INACTIVE') },
      { value: 'PENDING', label: this.translate.instant('ADMIN.STATUS_PENDING') },
      { value: 'BLOCKED', label: this.translate.instant('ADMIN.STATUS_BLOCKED') },
      { value: 'DELETED', label: this.translate.instant('ADMIN.STATUS_DELETED') }
    ];
  }

  get userRoles() {
    return [
      { value: 'USER', label: this.translate.instant('ADMIN.ROLE_USER') },
      { value: 'TRAINER', label: this.translate.instant('ADMIN.ROLE_TRAINER') },
      { value: 'ADMIN', label: this.translate.instant('ADMIN.ROLE_ADMIN') }
    ];
  }

  get subscriptionPlans() {
    return [
      { value: 'FREE', label: this.translate.instant('ADMIN.SUB_FREE') },
      { value: 'PRO', label: this.translate.instant('ADMIN.SUB_PRO') }
    ];
  }

  get genders() {
    return [
      { value: 'male', label: this.translate.instant('ADMIN.GENDER_MALE') },
      { value: 'female', label: this.translate.instant('ADMIN.GENDER_FEMALE') },
      { value: 'other', label: this.translate.instant('ADMIN.GENDER_OTHER') }
    ];
  }

  form = this.fb.group({
    // Account
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    firstName: [''],
    lastName: [''],
    // Role & Status
    status: [''],
    role: [''],
    subscriptionPlan: ['FREE'],
    subscriptionExpiresAt: [null as string | null],
    // Body
    dateOfBirth: [null as string | null],
    gender: [null as string | null],
    heightCm: [null as number | null],
    weightKg: [null as number | null],
    // Running
    maxHeartRate: [null as number | null],
    hrRest: [null as number | null],
    // Feature toggles
    asthmaTrackingEnabled: [false],
    cycleTrackingEnabled: [false],
    communityRoutesEnabled: [false],
    groupEventsEnabled: [false],
    discoverableByOthers: [false],
    dwdRegionId: [null as number | null]
  });

  displayId(): number | null { return this.userId(); }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = parseInt(idParam, 10);
      this.userId.set(id);
      this.loadUser(id);
    }
  }

  /** Pretty-print pace reference (e.g. "10K — 42:30"). */
  paceRefDisplay(): string | null {
    const u = this.loaded();
    if (!u || u.paceRefTimeSeconds == null) return null;
    const secs = u.paceRefTimeSeconds;
    const h = Math.floor(secs / 3600);
    const m = Math.floor((secs % 3600) / 60);
    const s = secs % 60;
    const time = h > 0
      ? `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
      : `${m}:${String(s).padStart(2, '0')}`;
    const label = u.paceRefLabel || (u.paceRefDistanceM != null ? `${(u.paceRefDistanceM / 1000).toFixed(1)} km` : '');
    return label ? `${label} — ${time}` : time;
  }

  /** Pretty-print threshold pace (sec/km -> m:ss/km). */
  thresholdPaceDisplay(): string | null {
    const u = this.loaded();
    if (!u || u.thresholdPaceSecPerKm == null) return null;
    const secs = u.thresholdPaceSecPerKm;
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}:${String(s).padStart(2, '0')} /km`;
  }

  private loadUser(id: number): void {
    this.isLoading.set(true);
    this.hasError.set(false);
    this.userService.getUserById(id).subscribe({
      next: (u) => {
        this.loaded.set(u);
        this.form.patchValue({
          username: u.username ?? '',
          email: u.email ?? '',
          firstName: u.firstName ?? '',
          lastName: u.lastName ?? '',
          status: u.status ?? '',
          role: u.role ?? '',
          maxHeartRate: u.maxHeartRate,
          hrRest: u.hrRest,
          subscriptionPlan: u.subscriptionPlan ?? 'FREE',
          subscriptionExpiresAt: u.subscriptionExpiresAt ? u.subscriptionExpiresAt.slice(0, 10) : null,
          dateOfBirth: u.dateOfBirth ?? null,
          gender: u.gender ?? null,
          heightCm: u.heightCm,
          weightKg: u.weightKg,
          asthmaTrackingEnabled: !!u.asthmaTrackingEnabled,
          cycleTrackingEnabled: !!u.cycleTrackingEnabled,
          communityRoutesEnabled: !!u.communityRoutesEnabled,
          groupEventsEnabled: !!u.groupEventsEnabled,
          discoverableByOthers: !!u.discoverableByOthers,
          dwdRegionId: u.dwdRegionId
        });
      },
      error: () => { this.hasError.set(true); this.isLoading.set(false); },
      complete: () => this.isLoading.set(false)
    });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const payload = {
      username: v.username ?? undefined,
      email: v.email ?? undefined,
      firstName: v.firstName || undefined,
      lastName: v.lastName || undefined,
      status: v.status || undefined,
      role: v.role || undefined,
      maxHeartRate: v.maxHeartRate ?? null,
      hrRest: v.hrRest ?? null,
      subscriptionPlan: v.subscriptionPlan || 'FREE',
      subscriptionExpiresAt: v.subscriptionExpiresAt ? v.subscriptionExpiresAt + 'T00:00:00' : null,
      dateOfBirth: v.dateOfBirth || null,
      gender: v.gender || null,
      heightCm: v.heightCm ?? null,
      weightKg: v.weightKg ?? null,
      asthmaTrackingEnabled: !!v.asthmaTrackingEnabled,
      cycleTrackingEnabled: !!v.cycleTrackingEnabled,
      communityRoutesEnabled: !!v.communityRoutesEnabled,
      groupEventsEnabled: !!v.groupEventsEnabled,
      discoverableByOthers: !!v.discoverableByOthers,
      dwdRegionId: v.asthmaTrackingEnabled ? (v.dwdRegionId ?? null) : null
    };
    this.isSaving.set(true);
    this.userService.updateUserAsAdmin(this.userId()!, payload).subscribe({
      next: () => this.router.navigate(['/admin/users']),
      error: () => { this.hasError.set(true); this.isSaving.set(false); },
      complete: () => this.isSaving.set(false)
    });
  }

  cancel(): void { this.router.navigate(['/admin/users']); }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';

import { UserService } from '../../../../services/user.service';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './user-form.html',
  styleUrl: './user-form.scss'
})
export class UserForm implements OnInit {
  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  userId = signal<number | null>(null);
  isLoading = signal(false);
  isSaving = signal(false);
  hasError = signal(false);

  readonly userStatuses = [
    { value: 'ACTIVE', label: 'Active' },
    { value: 'INACTIVE', label: 'Inactive' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'BLOCKED', label: 'Blocked' },
    { value: 'DELETED', label: 'Deleted' }
  ];

  readonly userRoles = [
    { value: 'USER', label: 'User' },
    { value: 'TRAINER', label: 'Trainer' },
    { value: 'ADMIN', label: 'Admin' }
  ];

  form = this.fb.group({
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    firstName: [''],
    lastName: [''],
    status: [''],
    role: [''],
    maxHeartRate: [null as number | null],
    hrRest: [null as number | null]
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

  private loadUser(id: number): void {
    this.isLoading.set(true);
    this.hasError.set(false);
    this.userService.getUserById(id).subscribe({
      next: (u) => {
        this.form.patchValue({
          username: u.username ?? '',
          email: u.email ?? '',
          firstName: u.firstName ?? '',
          lastName: u.lastName ?? '',
          status: u.status ?? '',
          role: u.role ?? '',
          maxHeartRate: u.maxHeartRate,
          hrRest: u.hrRest
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
      hrRest: v.hrRest ?? null
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

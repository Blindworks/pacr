import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subject, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import {
  AdminLoginMessageService,
  CreateLoginMessageRequest,
  LoginMessageTargetGroup,
  LoginMessageTargetType,
  UserSummary
} from '../../../../services/admin-login-message.service';
import { AdminUserService } from '../../../../services/admin-user.service';

const ALL_GROUPS: LoginMessageTargetGroup[] = ['PRO', 'FREE', 'TRAINER'];

@Component({
  selector: 'app-login-message-form',
  standalone: true,
  imports: [FormsModule, TranslateModule],
  templateUrl: './login-message-form.html',
  styleUrl: './login-message-form.scss'
})
export class LoginMessageForm implements OnInit {
  private messageService = inject(AdminLoginMessageService);
  private userService = inject(AdminUserService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private translate = inject(TranslateService);

  readonly availableGroups = ALL_GROUPS;

  messageId = signal<number | null>(null);
  title = signal('');
  content = signal('');
  saving = signal(false);
  error = signal('');

  targetType = signal<LoginMessageTargetType>('ALL');
  selectedGroups = signal<Set<LoginMessageTargetGroup>>(new Set());
  selectedUsers = signal<UserSummary[]>([]);

  userQuery = signal('');
  searchResults = signal<UserSummary[]>([]);
  searching = signal(false);

  private searchInput$ = new Subject<string>();

  ngOnInit(): void {
    this.searchInput$
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap(q => {
          const term = (q ?? '').trim();
          if (term.length < 2) {
            this.searching.set(false);
            return of<UserSummary[]>([]);
          }
          this.searching.set(true);
          return this.userService.searchUsers(term);
        })
      )
      .subscribe({
        next: results => {
          this.searching.set(false);
          const selectedIds = new Set(this.selectedUsers().map(u => u.id));
          this.searchResults.set(results.filter(r => !selectedIds.has(r.id)));
        },
        error: err => {
          console.error('User search failed', err);
          this.searching.set(false);
          this.searchResults.set([]);
        }
      });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.messageId.set(+id);
      this.messageService.getAll().subscribe({
        next: list => {
          const item = list.find(n => n.id === +id);
          if (item) {
            this.title.set(item.title);
            this.content.set(item.content);
            this.targetType.set(item.targetType ?? 'ALL');
            this.selectedGroups.set(new Set(item.targetGroups ?? []));
            this.selectedUsers.set([...(item.targetUsers ?? [])]);
          }
        }
      });
    } else {
      const preselectUserId = this.route.snapshot.queryParamMap.get('userId');
      if (preselectUserId) {
        const uid = +preselectUserId;
        if (!Number.isNaN(uid)) {
          this.targetType.set('USERS');
          this.userService.getUserSummary(uid).subscribe({
            next: user => {
              if (!this.selectedUsers().some(u => u.id === user.id)) {
                this.selectedUsers.set([...this.selectedUsers(), user]);
              }
            },
            error: err => console.error('Failed to preload user for login message', err)
          });
        }
      }
    }
  }

  setTargetType(type: LoginMessageTargetType): void {
    this.targetType.set(type);
    this.error.set('');
  }

  toggleGroup(group: LoginMessageTargetGroup): void {
    const next = new Set(this.selectedGroups());
    if (next.has(group)) {
      next.delete(group);
    } else {
      next.add(group);
    }
    this.selectedGroups.set(next);
  }

  hasGroup(group: LoginMessageTargetGroup): boolean {
    return this.selectedGroups().has(group);
  }

  onUserQueryChange(value: string): void {
    this.userQuery.set(value);
    this.searchInput$.next(value);
  }

  addUser(user: UserSummary): void {
    if (this.selectedUsers().some(u => u.id === user.id)) {
      return;
    }
    this.selectedUsers.set([...this.selectedUsers(), user]);
    this.searchResults.set(this.searchResults().filter(r => r.id !== user.id));
    this.userQuery.set('');
    this.searchResults.set([]);
  }

  removeUser(user: UserSummary): void {
    this.selectedUsers.set(this.selectedUsers().filter(u => u.id !== user.id));
  }

  save(): void {
    if (!this.title() || !this.content()) {
      this.error.set(this.translate.instant('ADMIN.LOGIN_MSG_REQUIRED'));
      return;
    }

    const type = this.targetType();
    if (type === 'GROUPS' && this.selectedGroups().size === 0) {
      this.error.set(this.translate.instant('ADMIN.LOGIN_MSG_TARGET_GROUPS_REQUIRED'));
      return;
    }
    if (type === 'USERS' && this.selectedUsers().length === 0) {
      this.error.set(this.translate.instant('ADMIN.LOGIN_MSG_TARGET_USERS_REQUIRED'));
      return;
    }

    this.saving.set(true);
    this.error.set('');

    const data: CreateLoginMessageRequest = {
      title: this.title(),
      content: this.content(),
      targetType: type,
      targetGroups: type === 'GROUPS' ? Array.from(this.selectedGroups()) : [],
      targetUserIds: type === 'USERS' ? this.selectedUsers().map(u => u.id) : []
    };
    const id = this.messageId();
    const call = id ? this.messageService.update(id, data) : this.messageService.create(data);

    call.subscribe({
      next: () => this.router.navigate(['/admin/login-messages']),
      error: () => {
        this.error.set(this.translate.instant('ADMIN.LOGIN_MSG_SAVE_ERROR'));
        this.saving.set(false);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/login-messages']);
  }
}

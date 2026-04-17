import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AdminLoginMessageService, LoginMessage } from '../../../../services/admin-login-message.service';

@Component({
  selector: 'app-login-message-list',
  standalone: true,
  imports: [DatePipe, TranslateModule],
  templateUrl: './login-message-list.html',
  styleUrl: './login-message-list.scss'
})
export class LoginMessageList implements OnInit {
  private messageService = inject(AdminLoginMessageService);
  private router = inject(Router);
  private translate = inject(TranslateService);

  messages = signal<LoginMessage[]>([]);
  isLoading = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.messageService.getAll().subscribe({
      next: data => { this.messages.set(data); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  goNew(): void {
    this.router.navigate(['/admin/login-messages/new']);
  }

  edit(item: LoginMessage): void {
    this.router.navigate(['/admin/login-messages', item.id, 'edit']);
  }

  publish(item: LoginMessage): void {
    this.messageService.publish(item.id).subscribe({
      next: () => this.load()
    });
  }

  unpublish(item: LoginMessage): void {
    this.messageService.unpublish(item.id).subscribe({
      next: () => this.load()
    });
  }

  targetLabel(item: LoginMessage): string {
    const type = item.targetType ?? 'ALL';
    if (type === 'ALL') {
      return this.translate.instant('ADMIN.LOGIN_MSG_TARGET_BADGE_ALL');
    }
    if (type === 'GROUPS') {
      const groups = (item.targetGroups ?? [])
        .map(g => this.translate.instant('ADMIN.LOGIN_MSG_GROUP_' + g));
      return groups.length ? groups.join(' + ') : this.translate.instant('ADMIN.LOGIN_MSG_TARGET_BADGE_ALL');
    }
    const count = item.targetUsers?.length ?? 0;
    return this.translate.instant('ADMIN.LOGIN_MSG_TARGET_BADGE_USERS_COUNT', { count });
  }

  delete(item: LoginMessage): void {
    if (!confirm(`"${item.title}" wirklich löschen?`)) return;
    this.messageService.delete(item.id).subscribe({
      next: () => this.load()
    });
  }
}

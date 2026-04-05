import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
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

  delete(item: LoginMessage): void {
    if (!confirm(`"${item.title}" wirklich löschen?`)) return;
    this.messageService.delete(item.id).subscribe({
      next: () => this.load()
    });
  }
}

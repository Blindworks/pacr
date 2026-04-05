import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AdminLoginMessageService, CreateLoginMessageRequest } from '../../../../services/admin-login-message.service';

@Component({
  selector: 'app-login-message-form',
  standalone: true,
  imports: [FormsModule, TranslateModule],
  templateUrl: './login-message-form.html',
  styleUrl: './login-message-form.scss'
})
export class LoginMessageForm implements OnInit {
  private messageService = inject(AdminLoginMessageService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private translate = inject(TranslateService);

  messageId = signal<number | null>(null);
  title = signal('');
  content = signal('');
  saving = signal(false);
  error = signal('');

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.messageId.set(+id);
      this.messageService.getAll().subscribe({
        next: list => {
          const item = list.find(n => n.id === +id);
          if (item) {
            this.title.set(item.title);
            this.content.set(item.content);
          }
        }
      });
    }
  }

  save(): void {
    if (!this.title() || !this.content()) {
      this.error.set(this.translate.instant('ADMIN.LOGIN_MSG_REQUIRED'));
      return;
    }

    this.saving.set(true);
    this.error.set('');

    const data: CreateLoginMessageRequest = { title: this.title(), content: this.content() };
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

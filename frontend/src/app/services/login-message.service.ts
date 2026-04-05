import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { apiUrl } from '../core/api-base';

export interface PendingLoginMessage {
  id: number;
  title: string;
  content: string;
}

@Injectable({ providedIn: 'root' })
export class LoginMessageService {
  private readonly http = inject(HttpClient);
  private readonly _messages = signal<PendingLoginMessage[]>([]);
  private _initialTotal = 0;

  readonly messages = this._messages.asReadonly();
  readonly currentMessage = computed(() => this._messages()[0] ?? null);
  readonly hasMessages = computed(() => this._messages().length > 0);

  get initialTotal(): number {
    return this._initialTotal;
  }

  reset(): void {
    this._messages.set([]);
    this._initialTotal = 0;
  }

  fetchPending(): void {
    this.http.get<PendingLoginMessage[]>(apiUrl('/login-messages/pending')).subscribe({
      next: messages => {
        this._initialTotal = messages.length;
        this._messages.set(messages);
      },
      error: () => { /* silent */ }
    });
  }

  dismissCurrent(): void {
    const current = this.currentMessage();
    if (!current) return;
    this.http.post(apiUrl(`/login-messages/${current.id}/dismiss`), {}).subscribe();
    this._messages.update(msgs => msgs.slice(1));
  }
}

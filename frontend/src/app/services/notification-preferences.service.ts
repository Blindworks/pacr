import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface NotificationPreferences {
  emailReminderEnabled: boolean;
  emailReminderTime: string;
  emailNewsEnabled: boolean;
}

@Injectable({ providedIn: 'root' })
export class NotificationPreferencesService {
  private readonly http = inject(HttpClient);
  private readonly base = 'http://localhost:8080/api/users/me/notification-preferences';

  getPreferences(): Observable<NotificationPreferences> {
    return this.http.get<NotificationPreferences>(this.base);
  }

  updatePreferences(prefs: NotificationPreferences): Observable<NotificationPreferences> {
    return this.http.put<NotificationPreferences>(this.base, prefs);
  }
}

import { Injectable, signal } from '@angular/core';
import { NewStravaActivityDto } from './dashboard.service';

@Injectable({ providedIn: 'root' })
export class NewStravaActivityDialogService {
  private readonly _activity = signal<NewStravaActivityDto | null>(null);
  readonly activity = this._activity.asReadonly();

  open(activity: NewStravaActivityDto): void { this._activity.set(activity); }
  close(): void { this._activity.set(null); }
}

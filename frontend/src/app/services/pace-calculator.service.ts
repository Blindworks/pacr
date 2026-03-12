import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class PaceCalculatorService {
  private readonly _open = signal(false);
  readonly isOpen = this._open.asReadonly();

  open(): void { this._open.set(true); }
  close(): void { this._open.set(false); }
}

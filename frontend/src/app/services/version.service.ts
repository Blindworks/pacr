import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { apiUrl } from '../core/api-base';

export interface VersionInfo {
  name: string;
  version: string;
  buildTimestamp: string;
}

@Injectable({ providedIn: 'root' })
export class VersionService {
  private readonly http = inject(HttpClient);
  private readonly _version = signal<VersionInfo | null>(null);
  readonly version = this._version.asReadonly();

  fetch(): void {
    this.http.get<VersionInfo>(apiUrl('/version')).subscribe({
      next: info => this._version.set(info),
      error: () => this._version.set({ name: 'PACR', version: 'unknown', buildTimestamp: '' })
    });
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

export interface GeocodingResult {
  lat: string;
  lon: string;
  display_name: string;
}

@Injectable({ providedIn: 'root' })
export class GeocodingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'https://nominatim.openstreetmap.org/search';

  geocode(query: string): Observable<GeocodingResult[]> {
    if (!query || query.trim().length < 2) {
      return of([]);
    }

    const headers = new HttpHeaders({
      'Accept': 'application/json'
    });

    return this.http.get<GeocodingResult[]>(this.baseUrl, {
      headers,
      params: {
        q: query.trim(),
        format: 'json',
        limit: '5'
      }
    }).pipe(
      catchError(() => of([]))
    );
  }
}

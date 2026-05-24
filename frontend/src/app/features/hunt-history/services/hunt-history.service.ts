import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  PagedResponse,
  SessionListFilter,
  SessionSummary,
} from '../../../core/models/session.model';

@Injectable({ providedIn: 'root' })
export class HuntHistoryService {
  private readonly http = inject(HttpClient);

  list(filter: SessionListFilter = {}): Observable<PagedResponse<SessionSummary>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filter)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<PagedResponse<SessionSummary>>('/api/sessions', { params });
  }
}

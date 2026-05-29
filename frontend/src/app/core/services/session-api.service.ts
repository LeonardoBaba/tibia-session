import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  PagedResponse,
  SessionDetail,
  SessionListFilter,
  SessionSummary,
} from '../models/session.model';

@Injectable({ providedIn: 'root' })
export class SessionApiService {
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

  getById(id: string): Observable<SessionDetail> {
    return this.http.get<SessionDetail>(`/api/sessions/${id}`);
  }

  update(id: string, patch: { name?: string; comment?: string }): Observable<SessionDetail> {
    return this.http.patch<SessionDetail>(`/api/sessions/${id}`, patch);
  }

  /**
   * Persiste a sessão. Requer autenticação (backend retorna 401 caso contrário).
   */
  create(payload: { input: string; name?: string; comment?: string }): Observable<SessionDetail> {
    return this.http.post<SessionDetail>('/api/sessions', payload);
  }

  /**
   * Processa o texto sem salvar — útil pra usuários não logados e pra fazer
   * "dry run" antes de salvar. A resposta tem o mesmo shape do detail, com
   * `id` nulo.
   */
  preview(input: string): Observable<SessionDetail> {
    return this.http.post<SessionDetail>('/api/sessions/preview', { input });
  }
}

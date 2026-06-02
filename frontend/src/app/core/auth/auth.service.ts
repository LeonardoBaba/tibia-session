import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { catchError, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthUser } from './user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly _currentUser = signal<AuthUser | null>(null);
  private readonly _loaded = signal(false);

  readonly currentUser = this._currentUser.asReadonly();
  readonly loaded = this._loaded.asReadonly();
  readonly isAuthenticated = computed(() => this._currentUser() !== null);

  loadCurrentUser(): void {
    this.http
      .get<AuthUser>('/api/auth/me')
      .pipe(
        tap((user) => this._currentUser.set(user)),
        catchError(() => {
          this._currentUser.set(null);
          return of(null);
        }),
      )
      .subscribe(() => this._loaded.set(true));
  }

  login(): void {
    const apiOrigin = this.resolveApiOrigin();
    window.location.href = `${apiOrigin}/oauth2/authorization/discord`;
  }

  logout(): void {
    this.http.post('/api/auth/logout', null).subscribe({
      next: () => {
        this._currentUser.set(null);
        window.location.assign('/');
      },
      error: () => {
        this._currentUser.set(null);
        window.location.assign('/');
      },
    });
  }

  private resolveApiOrigin(): string {
    const apiUrl = environment.apiUrl;
    if (apiUrl.startsWith('http')) {
      return apiUrl.replace(/\/api\/?$/, '');
    }
    return 'http://localhost:15600';
  }
}

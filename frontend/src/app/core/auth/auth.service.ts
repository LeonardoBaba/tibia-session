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

  /**
   * Carrega o usuário logado a partir do cookie de sessão. Chamado uma vez
   * no bootstrap. Em caso de 401, mantém `null` e marca como `loaded` mesmo
   * assim (pra UI deixar de mostrar skeleton).
   */
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

  /**
   * Redireciona o navegador pro endpoint do Spring que inicia o fluxo OAuth
   * do Discord. Quando o Discord redirecionar de volta, Spring cria a sessão
   * e o success handler redireciona pra `${frontendUrl}/`.
   */
  login(): void {
    // Em prod a apiUrl é absoluta. Em dev é "/api", então precisamos resolver
    // pra origem do servidor (proxy não funciona pra redirects de OAuth).
    const apiOrigin = this.resolveApiOrigin();
    window.location.href = `${apiOrigin}/oauth2/authorization/discord`;
  }

  logout(): void {
    this.http.post('/api/auth/logout', null).subscribe({
      next: () => {
        this._currentUser.set(null);
        // Recarrega pra reset total do estado da app.
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
      // Produção: apiUrl é absoluto.
      return apiUrl.replace(/\/api\/?$/, '');
    }
    // Dev: apiUrl é '/api'. Backend dev roda em localhost:15600 (proxy
    // do Angular não funciona pra OAuth porque exige redirect cross-origin).
    return 'http://localhost:15600';
  }
}

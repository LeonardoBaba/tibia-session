import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Anexa o cookie de sessão em chamadas pra `/api` (necessário pra cross-domain
 * em produção, e harmless em dev via proxy).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api') && !req.url.startsWith('http')) {
    return next(req);
  }
  return next(req.clone({ withCredentials: true }));
};

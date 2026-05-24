import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';

/**
 * Prepende `environment.apiUrl` em qualquer request que comece com `/api`
 * — assim os services chamam só `/api/sessions` e o interceptor resolve
 * pra dev (proxy) ou produção (URL absoluta) sem mudar nada.
 */
export const apiBaseUrlInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api')) {
    return next(req);
  }

  const url = environment.apiUrl + req.url.slice('/api'.length);
  return next(req.clone({ url }));
};

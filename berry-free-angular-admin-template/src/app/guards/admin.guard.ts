import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const role = auth.getRole();
  if (role === 'ADMIN') {
    return true;
  }
  router.navigateByUrl('/forbidden');
  return false;
};

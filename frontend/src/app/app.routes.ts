import { Routes } from '@angular/router';
import { CategoryList } from './pages/category/category-list/category-list';
import { CategoryShell } from './pages/category/category-shell/category-shell';
import { authGuard, guestGuard } from './guards/auth.guard';
import { LoginPage } from './pages/auth/login-page/login-page';
import { RegisterPage } from './pages/auth/register-page/register-page';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginPage,
    canActivate: [guestGuard],
  },
  {
    path: 'register',
    component: RegisterPage,
    canActivate: [guestGuard],
  },
  {
    path: 'categories',
    component: CategoryShell,
    canActivate: [authGuard],
    children: [
      { path: '', component: CategoryList },
    ],
  },
  { path: '', redirectTo: 'categories', pathMatch: 'full' },
  { path: '**', redirectTo: 'categories' },
];

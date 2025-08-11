import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AdminComponent } from './theme/layout/admin/admin.component';
import { GuestComponent } from './theme/layout/guest/guest.component';
import { ListTicketComponent } from './pages/tickets/list-ticket/list-ticket';
import { AddTicketComponent } from './pages/tickets/add-ticket/add-ticket';
import { EditTicketComponent } from './pages/tickets/edit-ticket/edit-ticket';
import { UserListComponent } from './pages/user/user-list/user-list';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

const routes: Routes = [
  {
    path: '',
    component: AdminComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: '/default', pathMatch: 'full' },
      { path: 'default', loadComponent: () => import('./demo/dashboard/default/default.component').then(m => m.DefaultComponent) },
      { path: 'tickets', component: ListTicketComponent },
      { path: 'ajout-ticket', component: AddTicketComponent },
      { path: 'modifier-ticket/:id', component: EditTicketComponent },
  { path: 'users', component: UserListComponent, canActivate: [adminGuard] },
      { path: 'forbidden', loadComponent: () => import('./demo/other/sample-page/sample-page.component').then(m => m.default) }
    ]
  },
  {
    path: '',
    component: GuestComponent,
    children: [
      {
        path: 'guest',
        loadChildren: () => import('./demo/pages/authentication/authentication.module').then(m => m.AuthenticationModule)
      },
      { path: '', redirectTo: '/guest/login', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: '/guest/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}

/**
 * Created by nrobison on 6/22/17.
 */
import {DashboardComponent} from './dashboard/dashboard.component';
import {LoggedInGuard} from '../user/LoggedInGuard';
import {PermissionsGuard} from '../user/permissions.guard';
import {ITrestleRoute} from '../navigation/navigation.module';
import {UsersComponent} from './users/users.component';
import {MetricsComponent} from './metrics/metrics.component';
import {IndexComponent} from './indicies/index.component';
import {Privileges} from '../user/trestle-user';

export const AdminRoutes: ITrestleRoute[] = [
  {path: '', redirectTo: 'dashboard', pathMatch: 'full'},
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [LoggedInGuard, PermissionsGuard],
    data: {roles: [Privileges.ADMIN]}
  },
  {
    path: 'users',
    component: UsersComponent,
    canActivate: [LoggedInGuard, PermissionsGuard],
    data: {roles: [Privileges.ADMIN]}
  },
  {
    path: 'metrics',
    component: MetricsComponent,
    canActivate: [LoggedInGuard, PermissionsGuard],
    data: {roles: [Privileges.ADMIN]}
  },
  {
    path: 'index',
    component: IndexComponent,
    canActivate: [LoggedInGuard, PermissionsGuard],
    data: {roles: [Privileges.DBA]}
  }
];

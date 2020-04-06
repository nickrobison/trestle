/**
 * Created by nrobison on 6/22/17.
 */
import { DashboardComponent } from "./dashboard/dashboard.component";
import {LoggedInGuard} from '../user/LoggedInGuard';
import {PermissionsGuard} from '../user/PermissionsGuard';
import {Privileges} from '../user/authentication.service';
import {ITrestleRoute} from '../navigation/navigation.module';
import {UsersComponent} from './users/users.component';

export const AdminRoutes: ITrestleRoute[] = [
    {path: "", redirectTo: "dashboard", pathMatch: "full"},
    {
        path: "dashboard",
        component: DashboardComponent,
        canActivate: [LoggedInGuard, PermissionsGuard],
        data: {roles: [Privileges.ADMIN]}
    },
    {
        path: "users",
        component: UsersComponent,
        canActivate: [LoggedInGuard, PermissionsGuard],
        data: {roles: [Privileges.ADMIN]}
    },
    // {
    //     path: "metrics",
    //     component: MetricsComponent,
    //     canActivate: [LoggedInGuard, PermissionsGuard],
    //     data: {roles: [Privileges.ADMIN]}
    // },
    // {
    //     path: "index",
    //     component: IndexComponent,
    //     canActivate: [LoggedInGuard, PermissionsGuard],
    //     data: {roles: [Privileges.DBA]}
    // }
];

/**
 * Created by nrobison on 6/22/17.
 */
import {ITrestleRoute} from "./NavigationModule/navigation.module";
import {DashboardComponent} from "./AdminModule/dashboard/app.dashboard";
import {LoggedInGuard} from "./UserModule/LoggedInGuard";
import {PermissionsGuard} from "./UserModule/PermissionsGuard";
import {Privileges} from "./UserModule/authentication.service";
import {UsersComponent} from "./AdminModule/users/users.component";
import {MetricsComponent} from "./AdminModule/metrics/metrics.component";

export const AdminRoutes: Array<ITrestleRoute> = [
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
    {
        path: "metrics",
        component: MetricsComponent,
        canActivate: [LoggedInGuard, PermissionsGuard],
        data: {roles: [Privileges.ADMIN]}
    },
];
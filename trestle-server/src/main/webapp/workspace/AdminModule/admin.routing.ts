/**
 * Created by nrobison on 6/22/17.
 */
import { ITrestleRoute } from "../NavigationModule/navigation.module";
import { DashboardComponent } from "./dashboard/app.dashboard";
import { LoggedInGuard } from "../UserModule/LoggedInGuard";
import { PermissionsGuard } from "../UserModule/PermissionsGuard";
import { Privileges } from "../UserModule/authentication.service";
import { UsersComponent } from "./users/users.component";
import { MetricsComponent } from "./metrics/metrics.component";
import { IndexComponent } from "./indicies/index.component";

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
    {
        path: "metrics",
        component: MetricsComponent,
        canActivate: [LoggedInGuard, PermissionsGuard],
        data: {roles: [Privileges.ADMIN]}
    },
    {
        path: "index",
        component: IndexComponent,
        canActivate: [LoggedInGuard, PermissionsGuard],
        data: {roles: [Privileges.DBA]}
    }
];

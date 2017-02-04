/**
 * Created by nrobison on 1/19/17.
 */
import {Route, RouterModule} from "@angular/router";
import {DashboardComponent} from "./dashboard/app.dashboard";
import {NgModule} from "@angular/core";
import {MaterialModule} from "@angular/material";
import {AdminComponent} from "./admin.component";
import {UsersComponent, UserAddDialog, MapValuesPipe} from "./users/users.component";
import {UserService} from "./users/users.service";
import {CommonModule} from "@angular/common";
import {LoggedInGuard} from "../LoggedInGuard";
import {PermissionsGuard} from "../PermissionsGuard";
import {Privileges} from "../authentication.service";
import {FormsModule} from "@angular/forms";

interface ITrestleRoute extends Route {
    data?: ITrestleRouteData
}

interface ITrestleRouteData {
    roles: Array<Privileges>;
}

const routes: Array<ITrestleRoute> = [
    {path: "", component: AdminComponent, children: [
        {path: "dashboard", component: DashboardComponent},
        {path: "users", component: UsersComponent, canActivate: [LoggedInGuard, PermissionsGuard], data: {roles: [Privileges.ADMIN]}},
        {path: "", redirectTo: "/dashboard", pathMatch: "full"}
    ]}
];

@NgModule({
    declarations: [DashboardComponent, AdminComponent, UsersComponent, UserAddDialog, MapValuesPipe,],
    imports: [
        MaterialModule,
        CommonModule,
        RouterModule.forChild(routes),
        FormsModule
    ],
    providers: [UserService],
    entryComponents: [UserAddDialog],
    bootstrap: [AdminComponent]
})

export class AdminModule {}

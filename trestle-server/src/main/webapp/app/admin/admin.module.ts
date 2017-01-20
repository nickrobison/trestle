/**
 * Created by nrobison on 1/19/17.
 */
import {Route, RouterModule} from "@angular/router";
import {DashboardComponent} from "./dashboard/app.dashboard";
import {NgModule} from "@angular/core";
import {MaterialModule} from "@angular/material";
import {AdminComponent} from "./admin.component";
import {UsersComponent} from "./users/users.component";
import {UserService} from "./users/users.service";
import {CommonModule} from "@angular/common";
import {MaterializeDirective} from "angular2-materialize";
import {AuthGuard} from "../../AuthGuard";

const routes: Array<Route> = [
    {path: "", component: AdminComponent, children: [
        {path: "dashboard", component: DashboardComponent},
        {path: "users", component: UsersComponent, canActivate: [AuthGuard]},
        {path: "", redirectTo: "/dashboard", pathMatch: "full"}
    ]}
];

@NgModule({
    declarations: [DashboardComponent, AdminComponent, UsersComponent],
    imports: [
        MaterialModule,
        CommonModule,
        RouterModule.forChild(routes)
    ],
    providers: [UserService],
    bootstrap: [AdminComponent]
})

export class AdminModule {}

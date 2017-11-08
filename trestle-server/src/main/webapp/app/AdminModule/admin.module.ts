/**
 * Created by nrobison on 1/19/17.
 */
import { RouterModule } from "@angular/router";
import { DashboardComponent } from "./dashboard/app.dashboard";
import { NgModule } from "@angular/core";
import { AdminComponent } from "./admin.component";
import { UsersComponent } from "./users/users.component";
import { CommonModule } from "@angular/common";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { UserAddDialog } from "./users/users.add.dialog";
import { MetricsComponent } from "./metrics/metrics.component";
import { MetricsService } from "./metrics/metrics.service";
import { MetricsGraph } from "./metrics/metrics-graph.component";
import { UserModule } from "../UserModule/user.module";
import { AdminRoutes } from "../admin.routing";
import { UIModule } from "../UIModule/ui.module";
import { MaterialModule } from "../MaterialModule/material.module";
import {SharedModule} from "../SharedModule/shared.module";

@NgModule({
    declarations: [DashboardComponent,
        AdminComponent,
        UsersComponent,
        UserAddDialog,
        MetricsComponent,
        MetricsGraph],
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        RouterModule.forChild(AdminRoutes),
        UserModule,
        UIModule,
        MaterialModule,
        SharedModule
    ],
    providers: [MetricsService],
    entryComponents: [UserAddDialog],
    bootstrap: [AdminComponent]
})

export class AdminModule {
}

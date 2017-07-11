/**
 * Created by nrobison on 1/19/17.
 */
import { RouterModule } from "@angular/router";
import { DashboardComponent } from "./dashboard/app.dashboard";
import { NgModule } from "@angular/core";
import { MaterialModule } from "@angular/material";
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
        MaterialModule,
        UserModule,
        UIModule
    ],
    providers: [MetricsService],
    entryComponents: [UserAddDialog],
    bootstrap: [AdminComponent]
})

export class AdminModule {
}

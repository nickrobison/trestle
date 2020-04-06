import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {AdminComponent} from './admin/admin.component';
import {RouterModule} from '@angular/router';
import {AdminRoutes} from './admin.routing';
import {DashboardComponent} from './dashboard/dashboard.component';
import {MaterialModule} from '../material/material.module';
import {UserModule} from '../user/user.module';
import {NavigationModule} from '../navigation/navigation.module';
import {UsersComponent} from './users/users.component';
import {UserDialogComponent} from './users/users.dialog.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {SharedModule} from '../shared/shared.module';
import {MetricsComponent} from './metrics/metrics.component';
import {MetricsGraphComponent} from './metrics-graph/metrics-graph.component';
import {MetricsService} from './metrics-graph/metrics.service';


@NgModule({
  declarations: [
    AdminComponent,
    DashboardComponent,
    MetricsComponent,
    MetricsGraphComponent,
    UsersComponent,
    UserDialogComponent
  ],
  providers: [MetricsService],
  imports: [
    CommonModule,
    UserModule,
    MaterialModule,
    UserModule,
    FormsModule,
    ReactiveFormsModule,
    NavigationModule,
    RouterModule.forChild(AdminRoutes),
    SharedModule
  ],
  entryComponents: [UserDialogComponent]
})
export class AdminModule {
}

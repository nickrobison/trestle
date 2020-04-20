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
import {IndexTableComponent} from './indicies/index-table/index-table.component';
import {TreeGraphComponent} from './indicies/tree-graph/tree-graph.component';
import {WarningDialogComponent} from './indicies/warning-dialog/warning-dialog-component';
import {IndexService} from './indicies/index.service';
import {IndexComponent} from './indicies/index.component';


@NgModule({
  declarations: [
    AdminComponent,
    DashboardComponent,
    MetricsComponent,
    MetricsGraphComponent,
    UsersComponent,
    UserDialogComponent,
    IndexTableComponent,
    TreeGraphComponent,
    WarningDialogComponent,
    IndexComponent
  ],
  providers: [MetricsService, IndexService],
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
  // This will not be needed once we enable Ivy
  entryComponents: [UserDialogComponent, WarningDialogComponent]
})
export class AdminModule {
}

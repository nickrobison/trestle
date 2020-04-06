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
import {ReactiveFormsModule} from '@angular/forms';
import {SharedModule} from '../shared/shared.module';


@NgModule({
  declarations: [AdminComponent, DashboardComponent, UsersComponent, UserDialogComponent],
  imports: [
    CommonModule,
    UserModule,
    MaterialModule,
    UserModule,
    ReactiveFormsModule,
    NavigationModule,
    RouterModule.forChild(AdminRoutes),
    SharedModule,
    SharedModule
  ],
  entryComponents: [UserDialogComponent]
})
export class AdminModule {
}

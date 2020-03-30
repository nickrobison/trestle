import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminComponent } from './admin/admin.component';
import {RouterModule} from '@angular/router';
import {AdminRoutes} from './admin.routing';
import { DashboardComponent } from './dashboard/dashboard.component';
import {MaterialModule} from '../material/material.module';
import {UserModule} from '../user/user.module';
import {NavigationModule} from '../navigation/navigation.module';



@NgModule({
  declarations: [AdminComponent, DashboardComponent],
  imports: [
    CommonModule,
    UserModule,
    MaterialModule,
    UserModule,
    NavigationModule,
    RouterModule.forChild(AdminRoutes)
  ]
})
export class AdminModule { }

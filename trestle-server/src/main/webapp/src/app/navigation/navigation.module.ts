import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationComponent } from './navigation/navigation.component';
import { LoginComponent } from './login/login.component';
import {MaterialModule} from "../material/material.module";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {Route, RouterModule} from '@angular/router';
import {Privileges} from '../user/trestle-user';
import { SidebarComponent } from './sidebar/sidebar.component';

export interface ITrestleRoute extends Route {
  data?: ITrestleRouteData
}

export interface ITrestleRouteData {
  roles: Privileges[];
}

@NgModule({
  declarations: [NavigationComponent, LoginComponent, SidebarComponent],
  imports: [
    CommonModule,
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule
  ],
  exports: [
    NavigationComponent,
    LoginComponent,
    SidebarComponent
  ]
})
export class NavigationModule { }

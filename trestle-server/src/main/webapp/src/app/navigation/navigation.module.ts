import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationComponent } from './navigation/navigation.component';
import { LoginComponent } from './login/login.component';
import {MaterialModule} from "../material/material.module";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {Route, RouterModule} from '@angular/router';
import {Privileges} from '../user/trestle-user';
import { SidebarComponent } from './sidebar/sidebar.component';
import { TopNavComponent } from './top-nav/top-nav.component';
import {FontAwesomeModule} from '@fortawesome/angular-fontawesome';
import { UserIconComponent } from './user-icon/user-icon.component';

export interface ITrestleRoute extends Route {
  data?: ITrestleRouteData
}

export interface ITrestleRouteData {
  roles: Privileges[];
}

@NgModule({
  declarations: [NavigationComponent, LoginComponent, SidebarComponent, TopNavComponent, UserIconComponent],
  imports: [
    CommonModule,
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    FontAwesomeModule
  ],
  exports: [
    NavigationComponent,
    LoginComponent,
    SidebarComponent,
    TopNavComponent
  ]
})
export class NavigationModule { }

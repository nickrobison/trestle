import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {NavigationComponent} from './navigation/navigation.component';
import {LoginComponent} from './login/login.component';
import {MaterialModule} from "../material/material.module";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {Route, RouterModule} from '@angular/router';
import {Privileges} from '../user/trestle-user';
import {SidebarComponent} from './sidebar/sidebar.component';
import {TopNavComponent} from './top-nav/top-nav.component';
import {FontAwesomeModule} from '@fortawesome/angular-fontawesome';
import {UserIconComponent} from './user-icon/user-icon.component';
import {UiModule} from "../ui/ui.module";
import {NotificationComponent} from "./notifications/notification/notification.component";
import {NotificationCenterComponent} from "./notifications/notification-center/notification-center.component";

export interface ITrestleRoute extends Route {
  data?: ITrestleRouteData;
}

export interface ITrestleRouteData {
  roles: Privileges[];
}

@NgModule({
  declarations: [
    LoginComponent,
    NavigationComponent,
    NotificationComponent,
    NotificationCenterComponent,
    SidebarComponent,
    TopNavComponent,
    UserIconComponent,
  ],
  imports: [
    CommonModule,
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    FontAwesomeModule,
    UiModule
  ],
  exports: [
    NavigationComponent,
    LoginComponent,
    SidebarComponent,
    TopNavComponent
  ]
})
export class NavigationModule {
}

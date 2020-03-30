import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {HttpClientModule} from '@angular/common/http';
import {AuthService} from './authentication.service';
import {LoggedInGuard} from './LoggedInGuard';
import {PermissionsGuard} from './PermissionsGuard';
import {DefaultRouteGuard} from './DefaultRouteGuard';
import {UserService} from './users.service';

@NgModule({
  declarations: [],
  imports: [
    HttpClientModule,
    CommonModule
  ],
  providers: [
    AuthService,
    LoggedInGuard,
    PermissionsGuard,
    DefaultRouteGuard,
    UserService
  ]
})
export class UserModule {
}

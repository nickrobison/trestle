import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {AuthService} from './authentication.service';
import {LoggedInGuard} from './LoggedInGuard';
import {PermissionsGuard} from './permissions.guard';
import {DefaultRouteGuard} from './DefaultRouteGuard';
import {UserService} from './users.service';

@NgModule({
  declarations: [],
  imports: [
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

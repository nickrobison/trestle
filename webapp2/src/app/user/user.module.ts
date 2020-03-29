import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {HttpClientModule} from "@angular/common/http";
import {JwtModule} from "@auth0/angular-jwt";
import {AuthService} from "./authentication.service";
import {LoggedInGuard} from "./LoggedInGuard";
import {PermissionsGuard} from "./PermissionsGuard";
import {DefaultRouteGuard} from "./DefaultRouteGuard";
import {UserService} from "./users.service";

const _key: string = "access_token";


@NgModule({
  declarations: [],
  imports: [
    HttpClientModule,
    CommonModule,
    JwtModule.forRoot({
      config: {
        tokenGetter: tokenGetter
      }
    })
  ],
  providers: [
    AuthService,
    LoggedInGuard,
    PermissionsGuard,
    DefaultRouteGuard,
    UserService
  ]
})
export class UserModule { }

export function tokenGetter() {
  return localStorage.getItem(_key);
}

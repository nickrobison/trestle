import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {HttpClientModule} from '@angular/common/http';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {UserModule} from './user/user.module';
import {MaterialModule} from './material/material.module';
import {NavigationModule} from './navigation/navigation.module';
import {FontAwesomeModule} from '@fortawesome/angular-fontawesome';
import {JWT_OPTIONS, JwtModule} from '@auth0/angular-jwt';
import {environment} from '../environments/environment';
import {AuthService} from './user/authentication.service';
import { StoreModule } from '@ngrx/store';
import { reducers, metaReducers } from './reducers';
import { StoreDevtoolsModule } from '@ngrx/store-devtools';
import { EffectsModule } from '@ngrx/effects';
import { AuthEffects } from './effects/auth.effects';

export function jwtOptionsFactory(service: AuthService) {
  // noinspection JSUnusedGlobalSymbols
  return {
    tokenGetter: () => {
      return service.getEncodedToken();
    },
    whitelistedDomains: [environment.domain],
    blacklistedRoutes: ['http://localhost:8080/auth/login'],
    throwNoTokenError: true,
    authScheme: ''
  };
}


@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    AppRoutingModule,
    BrowserModule,
    BrowserAnimationsModule,
    UserModule,
    MaterialModule,
    NavigationModule,
    FontAwesomeModule,
    JwtModule.forRoot({
      jwtOptionsProvider: {
        provide: JWT_OPTIONS,
        useFactory: jwtOptionsFactory,
        deps: [AuthService]
      }
    }),
    StoreModule.forRoot(reducers, {
      metaReducers, 
      runtimeChecks: {
        strictStateImmutability: true,
        strictActionImmutability: true,
      }
    }),
    !environment.production ? StoreDevtoolsModule.instrument() : [],
    EffectsModule.forFeature([AuthEffects]),
    EffectsModule.forRoot([])
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}

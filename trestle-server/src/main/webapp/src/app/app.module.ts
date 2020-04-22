import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {HttpClientModule} from '@angular/common/http';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {UserModule} from './user/user.module';
import {MaterialModule} from './material/material.module';
import {NavigationModule} from './navigation/navigation.module';
import {JWT_OPTIONS, JwtModule} from '@auth0/angular-jwt';
import {environment} from '../environments/environment';
import {select, Store, StoreModule} from '@ngrx/store';
import {metaReducers, reducers, selectTokenFromUser, State} from './reducers';
import {StoreDevtoolsModule} from '@ngrx/store-devtools';
import {EffectsModule} from '@ngrx/effects';
import {AuthEffects} from './effects/auth.effects';

export function jwtOptionsFactory(store: Store<State>) {
  const tokenSelector = store.pipe(select(selectTokenFromUser));
  return {
    tokenGetter: () => {
      // Awkward workaround for: https://github.com/auth0/angular2-jwt/issues/467
      return new Promise((resolve) => {
        tokenSelector.subscribe(token => resolve(token));
      });
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
    JwtModule.forRoot({
      jwtOptionsProvider: {
        provide: JWT_OPTIONS,
        useFactory: jwtOptionsFactory,
        deps: [Store]
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

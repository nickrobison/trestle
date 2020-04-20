import {Injectable} from '@angular/core';
import {Actions, createEffect, ofType} from '@ngrx/effects';
import {AuthService, TrestleToken} from '../user/authentication.service';
import {login, loginFailure, loginSuccess} from '../actions/auth.actions';
import {catchError, exhaustMap, map} from 'rxjs/operators';
import {TrestleUser} from '../user/trestle-user';
import {of} from 'rxjs';
import {JwtHelperService} from '@auth0/angular-jwt';


@Injectable()
export class AuthEffects {
  private jwtHelper: JwtHelperService;

  constructor(private actions$: Actions, private authService: AuthService) {
    this.jwtHelper = new JwtHelperService();
  }

  private getToken(jwtToken: string): TrestleToken {
    return new TrestleToken(this.jwtHelper.decodeToken(jwtToken));
  }

  login = createEffect(() =>
    this.actions$
      .pipe(ofType(login), exhaustMap(action =>
        this.authService.login(action.username, action.password)
          .pipe(map(this.getToken),
            map(token => new TrestleUser(token.getUser())),
            map(user => loginSuccess({user})),
            catchError(error => of(loginFailure({error}))))))
  );
}

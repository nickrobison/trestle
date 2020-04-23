import {Injectable} from '@angular/core';
import {Actions, createEffect, ofType} from '@ngrx/effects';
import {AuthService} from '../user/authentication.service';
import {login, loginFailure, loginSuccess, logout} from '../actions/auth.actions';
import {catchError, exhaustMap, map, tap} from 'rxjs/operators';
import {of} from 'rxjs';
import {Router} from '@angular/router';


@Injectable()
export class AuthEffects {

  constructor(private actions$: Actions, private authService: AuthService, private router: Router) {
    // Not used
  }


  login = createEffect(() =>
    this.actions$
      .pipe(ofType(login), exhaustMap(action => {
        return this.authService.login(action.username, action.password)
          .pipe(
            map(({user, token}) => {
              return loginSuccess({user, returnUrl: action.returnUrl, token});
            }),
            catchError(error => {
              return of(loginFailure({error}));
            }));
      }))
  );

  loginSuccess = createEffect(() =>
      this.actions$
        .pipe(ofType(loginSuccess), tap(action => {
          return this.router.navigate([action.returnUrl]);
        })), {
      dispatch: false
    }
  );

  logout = createEffect(() =>
    this.actions$
      .pipe(ofType(logout), exhaustMap(() => {
        return this.authService.logout().pipe(tap(() => {
          return this.router.navigate(['/']);
        }));
      })), {
    dispatch: false
  });
}

import {Injectable} from '@angular/core';
import {Actions, createEffect, ofType} from '@ngrx/effects';
import {AuthService} from '../user/authentication.service';
import {login, loginFailure, loginSuccess} from '../actions/auth.actions';
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
            map(user => {
              return loginSuccess({user, returnUrl: action.returnUrl});
            }),
            catchError(error => of(loginFailure({error}))));
      }))
  );

  loginSuccess = createEffect(() =>
      this.actions$
        .pipe(ofType(loginSuccess), tap(action => {
          console.debug('Success! Let\'s redirect');
          return this.router.navigate([action.returnUrl]);
          // if (action.user.isAdmin()) {
          //   return this.router.navigate(['admin', 'dashboard']);
          // } else {
          //   return this.router.navigate(['dashboard']);
          // }
        })), {
      dispatch: false
    }
  );
}

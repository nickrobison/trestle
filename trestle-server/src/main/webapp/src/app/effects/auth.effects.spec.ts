import {TestBed} from '@angular/core/testing';
import {provideMockActions} from '@ngrx/effects/testing';
import {Observable, of, throwError} from 'rxjs';

import {AuthEffects} from './auth.effects';
import {AuthService} from '../user/authentication.service';
import {login, loginFailure, loginSuccess, logout} from '../actions/auth.actions';
import {Privileges} from '../user/trestle-user';
import {UserModule} from '../user/user.module';
import {RouterTestingModule} from '@angular/router/testing';
import {HttpClientTestingModule} from '@angular/common/http/testing';
import {cold} from 'jest-marbles';
import {Router} from '@angular/router';
import {createMockUser} from '../../test.helpers';

describe('AuthEffects', () => {
  let actions$: Observable<any>;
  let effects: AuthEffects;
  let authService: AuthService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        AuthEffects,
        provideMockActions(() => actions$)
      ],
      imports: [UserModule, RouterTestingModule, HttpClientTestingModule]
    });

    effects = TestBed.inject(AuthEffects);
  });

  it('should be created', () => {
    expect(effects).toBeTruthy();
  });

  it('should dispatch success', () => {
    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    const testUser = createMockUser(Privileges.USER);
    spyOn(authService, 'login').and.returnValue(of({user: testUser, token: ""}));

    actions$ = cold('a', {
      a: login({username: '', password: '', returnUrl: ''})
    });

    const expected = cold('a', {
      a: loginSuccess({user: testUser, returnUrl: '', token: ""})
    });

    expect(effects.login).toBeObservable(expected);
  });

  it('should dispatch failure', () => {
    authService = TestBed.inject(AuthService);
    spyOn(authService, 'login').and.returnValue(throwError(new Error('Bad')));

    actions$ = cold('a', {
      a: login({username: '', password: '', returnUrl: ''})
    });

    const expected = cold('a', {
      a: loginFailure({error: new Error('Bad')})
    });

    expect(effects.login).toBeObservable(expected);
  });

  it('should navigate on success', () => {
    const testUser = createMockUser(Privileges.USER);
    router = TestBed.inject(Router);
    const navSpy = spyOn(router, 'navigate');
    actions$ = of(loginSuccess({user: testUser, returnUrl: '/hello', token: ""}));

    effects.loginSuccess.subscribe(() => {
      expect(navSpy).toBeCalledWith(['/hello']);
    });
  });

  it('should navigate on logout', () => {
    router = TestBed.inject(Router);
    const navSpy = spyOn(router, 'navigate');
    actions$ = of(logout);

    effects.logout.subscribe(() => {
      expect(navSpy).toBeCalledWith(['/']);
    });
  });
});

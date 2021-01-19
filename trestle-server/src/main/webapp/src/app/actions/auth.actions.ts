import {createAction, props} from '@ngrx/store';
import {TrestleUser} from '../user/trestle-user';

export const login = createAction(
  '[Auth] Login',
  props<{ username: string, password: string, returnUrl: string }>()
);

export const loginSuccess = createAction(
  '[Auth] Login Success',
  props<{ user: TrestleUser, returnUrl: string, token: string }>()
);

export const loginFailure = createAction(
  '[Auth] Login Failure',
  props<{ error: Error }>()
);

export const logout = createAction(
  '[Auth] Logout'
);

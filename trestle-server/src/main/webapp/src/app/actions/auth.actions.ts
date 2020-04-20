import { createAction, props } from '@ngrx/store';
import {TrestleUser} from '../user/trestle-user';

export const login = createAction(
  '[Auth] Login',
  props<{username: string, password: string}>()
);

export const loginSuccess = createAction(
  '[Auth] Login Success',
  props<{ user: TrestleUser }>()
);

export const loginFailure = createAction(
  '[Auth] Load Auths Failure',
  props<{ error: any }>()
);

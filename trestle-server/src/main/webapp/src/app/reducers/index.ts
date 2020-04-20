import {ActionReducerMap, createReducer, MetaReducer, on} from '@ngrx/store';
import {environment} from '../../environments/environment';
import {TrestleUser} from '../user/trestle-user';
import {login, loginFailure, loginSuccess} from '../actions/auth.actions';

export interface UserState {
  user: TrestleUser;
  userError: Error;
}

const initialUserState: UserState = {
  user: null,
  userError: null
};

export interface State {
  user: UserState;
}

const _authReducer = createReducer(initialUserState, on(login, state => {
  console.log('Trying to login');
  return state;
}),
  on(loginSuccess, (state, {user}) => ({
    userError: null,
    user
  })),
  on(loginFailure, (state, {error}) =>({
    user: null,
    userError: error
  })));


export function authReducer(state: UserState = initialUserState, action): UserState {
  return _authReducer(state, action);
}

export const reducers: ActionReducerMap<State> = {
  user: authReducer
};


export const metaReducers: MetaReducer<State>[] = !environment.production ? [] : [];

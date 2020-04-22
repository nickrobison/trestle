import {ActionReducerMap, createReducer, createSelector, MetaReducer, on} from '@ngrx/store';
import {environment} from '../../environments/environment';
import {TrestleUser} from '../user/trestle-user';
import {login, loginFailure, loginSuccess, logout} from '../actions/auth.actions';

export interface UserState {
  user: TrestleUser;
  userError: Error;
  userToken: string;
}

const initialUserState: UserState = {
  user: null,
  userError: null,
  userToken: ''
};

export interface State {
  user: UserState;
}

const _authReducer = createReducer(initialUserState, on(login, state => {
    console.log('Trying to login');
    return state;
  }),
  on(loginSuccess, (state, {user, token}) => ({
    userError: null,
    user,
    userToken: token
  })),
  on(loginFailure, (state, {error}) => ({
    user: null,
    userError: error,
    userToken: ''
  })),
  on(logout, (state) => ({
    ...state,
    user: null,
    token: ''
  })));


export function authReducer(state: UserState = initialUserState, action): UserState {
  return _authReducer(state, action);
}

export const reducers: ActionReducerMap<State> = {
  user: authReducer
};


export const metaReducers: MetaReducer<State>[] = !environment.production ? [] : [];


export const selectUserState = (state: State) => state.user;
export const selectUser = (state: UserState) => state.user;


export const selectUserFromUser = createSelector(
  selectUserState,
  selectUser);

export const selectErrorFromUser = createSelector(
  selectUserState,
  (state) => state.userError
);

export const selectTokenFromUser = createSelector(
  selectUserState,
  (state) => state.userToken
);

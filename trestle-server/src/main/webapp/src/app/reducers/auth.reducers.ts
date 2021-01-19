import {createReducer, createSelector, on} from "@ngrx/store";
import {login, loginFailure, loginSuccess, logout} from "../actions/auth.actions";
import {TrestleUser} from "../user/trestle-user";
import {State} from "./index";

export interface UserState {
  user: TrestleUser;
  userError: Error;
  userToken: string;
}

export const initialUserState: UserState = {
  user: null,
  userError: null,
  userToken: ''
};

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
    userToken: ''
  })));


export function authReducer(state: UserState = initialUserState, action): UserState {
  return _authReducer(state, action);
}

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

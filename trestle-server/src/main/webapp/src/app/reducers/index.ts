import {ActionReducerMap, MetaReducer} from '@ngrx/store';
import {storageMetaReducer} from './storage.metareducer';
import {authReducer, UserState} from "./auth.reducers";

export interface State {
  user: UserState;
}

export const reducers: ActionReducerMap<State> = {
  user: authReducer
};

export const metaReducers: MetaReducer<State>[] = [storageMetaReducer];

import {ActionReducerMap, MetaReducer} from '@ngrx/store';
import {storageMetaReducer} from './storage.metareducer';
import {authReducer, UserState} from "./auth.reducers";
import {notificationReducer, NotificationState} from "./notification.reducers";

export interface State {
  user: UserState;
  notifications: NotificationState;

}

export const reducers: ActionReducerMap<State> = {
  user: authReducer,
  notifications: notificationReducer,
};

export const metaReducers: MetaReducer<State>[] = [storageMetaReducer];

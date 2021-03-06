import {ActionReducerMap, MetaReducer} from '@ngrx/store';
import {storageMetaReducer} from './storage.metareducer';
import {authReducer, initialUserState, UserState} from './auth.reducers';
import {initialNotificationState, notificationReducer, NotificationState} from './notification.reducers';

export interface State {
  user: UserState;
  notifications: NotificationState;
}

export const initialAppState: State = {
  user: initialUserState,
  notifications: initialNotificationState,
};

export const reducers: ActionReducerMap<State> = {
  user: authReducer,
  notifications: notificationReducer,
};

export const metaReducers: MetaReducer<State>[] = [storageMetaReducer];

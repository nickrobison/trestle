import {createAction, props} from '@ngrx/store';
import {TrestleNotification} from "../reducers/notification.reducers";

export const loadNotifications = createAction(
  '[Notification] Load Notifications'
);

export const addNotification = createAction(
  '[Notification] Add Notification',
  props<{notification: TrestleNotification}>()
);

export const removeNotification = createAction(
  '[Notification] Remove Notification',
  props<{notification: TrestleNotification}>()
);

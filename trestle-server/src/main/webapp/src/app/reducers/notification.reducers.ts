import {createReducer, createSelector, on} from "@ngrx/store";
import {addNotification, removeNotification} from "../actions/notification.actions";
import {State} from "./index";

export interface TrestleMessage {
  state: "notification";
  msg: string
}

export interface TrestleError {
  state: "error";
  error: Error;
}

export type TrestleNotification = TrestleMessage | TrestleError;

export type NotificationState = {
  notifications: TrestleNotification[];
};

const initialNotificationState: NotificationState = {
  notifications: [],
};

const _notificationReducer = createReducer(initialNotificationState,
  on(addNotification, (state, news) => {
    console.debug("Adding notification");
    return {
      ...state,
      notifications: [...state.notifications, news.notification]
    };
  }),
  on(removeNotification, (state, news) => {
    const notices = state.notifications;
    const idx = notices.indexOf(news.notification);
    if (idx > -1) {
      notices.splice(idx, 1);
    }
    return {
      ...state,
      notifications: notices,
    };
  }));

export function notificationReducer(state: NotificationState = initialNotificationState, action): NotificationState {
  return _notificationReducer(state, action);
}

// Selectors

export const selectNotificationState = (state: State) => state.notifications;

export const selectNotificationsFromNotifications = createSelector(
  selectNotificationState,
  (state) => state.notifications
);

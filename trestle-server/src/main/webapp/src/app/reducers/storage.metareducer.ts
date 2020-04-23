import {Action, ActionReducer} from '@ngrx/store';
import {ITrestleUser} from '../user/authentication.service';
import {TrestleUser} from '../user/trestle-user';
import merge from "lodash/merge";
import pick from "lodash/pick";

function isSerializedUser(value: any): value is ITrestleUser {
  return (value as ITrestleUser).email !== undefined;
}

function setSavedState(state: any, localStorageKey: string) {
  localStorage.setItem(localStorageKey, JSON.stringify(state, (key, value) => {
    console.debug('Replace:', key, value);
    if (value instanceof TrestleUser) {
      console.debug('Is User');
      return value.serialize();
    }
    return value;
  }));
}

function getSavedState(key: string): any {
  return JSON.parse(localStorage.getItem(key), (key, value) => {
    console.debug('KV', key, value);
    if (value !== null && isSerializedUser(value)) {
      console.debug('Is serialized user');
      return new TrestleUser(value);
    }
    return value;
  });
}

const stateKeys = ['user.user', 'user.userToken'];
const localStorageKey = '__trestle__';

export function storageMetaReducer<S, A extends Action = Action>(reducer: ActionReducer<S, A>) {
  let onInit = true;
  return function(state: S, action: A) {
    console.debug('Storage meta');
    const nextState = reducer(state, action);

    if (onInit) {
      console.debug('Getting state');
      onInit = false;
      const savedState = getSavedState(localStorageKey);
      console.debug('Saved state', savedState);
      return merge(nextState, savedState);
    }

    const stateToSave = pick(nextState, stateKeys);
    setSavedState(stateToSave, localStorageKey);
    return nextState;
  };
}

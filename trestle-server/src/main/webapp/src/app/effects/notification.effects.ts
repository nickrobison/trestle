import {Injectable} from '@angular/core';
import {Actions, createEffect, ofType} from '@ngrx/effects';
import {NotificationService} from "../ui/notifications/notification.service";
import {addNotification, removeNotification} from "../actions/notification.actions";
import {tap} from "rxjs/operators";


@Injectable()
export class NotificationEffects {


  constructor(private actions$: Actions, private notificationService: NotificationService) {
  }

  addNotification = createEffect(() => this.actions$
    .pipe(ofType(addNotification), tap(action => {
      console.debug("Handling notification effect: ", action.notification);
      return this.notificationService.addNotification(action.notification);
    })), {
    dispatch: false
  });

  removeNotification = createEffect(() => this.actions$
    .pipe(ofType(removeNotification), tap(action => {
      return this.notificationService.removeNotification(action.notification);
    })), {
    dispatch: false
  });
}

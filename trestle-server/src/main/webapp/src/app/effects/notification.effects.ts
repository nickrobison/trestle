import {Injectable} from '@angular/core';
import {Actions} from '@ngrx/effects';
import {NotificationService} from "../navigation/notifications/notification.service";


@Injectable()
export class NotificationEffects {


  constructor(private actions$: Actions, private notificationService: NotificationService) {
  }
  //
  // addNotification = createEffect(() => this.actions$
  //   .pipe(ofType(addNotification), tap(action => {
  //     console.debug("Handling notification effect: ", action.notification);
  //     return this.notificationService.addNotification(action.notification);
  //   })), {
  //   dispatch: false
  // });
  //
  // removeNotification = createEffect(() => this.actions$
  //   .pipe(ofType(removeNotification), tap(action => {
  //     return this.notificationService.removeNotification(action.notification);
  //   })), {
  //   dispatch: false
  // });
}

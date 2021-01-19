import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {TrestleNotification} from "../../../reducers/notification.reducers";
import {Store} from "@ngrx/store";
import {State} from "../../../reducers";
import {removeNotification} from "../../../actions/notification.actions";
import {notificationAnimations, NotificationAnimationState, NotificationButtonState} from "./notification-animations";
import {AnimationEvent} from "@angular/animations";

@Component({
  selector: 'app-toast',
  templateUrl: './notification.component.html',
  styleUrls: ['./notification.component.scss'],
  animations: [notificationAnimations.fadeNotification, notificationAnimations.buttonNotification],
})
export class NotificationComponent implements OnInit, OnDestroy {

  @Input()
  notification: TrestleNotification;
  readonly notificationLifetime = 5;
  animationState: NotificationAnimationState = 'default';
  buttonState: NotificationButtonState = 'out';
  private intervalId: number;

  constructor(private store: Store<State>) {
  }

  ngOnInit(): void {
    this.intervalId = setTimeout(() => this.triggerClose(), this.notificationLifetime * 1000);
  }

  ngOnDestroy() {
    clearTimeout(this.intervalId);
  }

  onFadeFinished(event: AnimationEvent) {
    const {toState} = event;
    const isFadeOut = (toState as NotificationAnimationState) === 'closing';
    const itFinished = this.animationState === 'closing';
    if (itFinished && isFadeOut) {
      this.close();
    }
  }

  triggerClose() {
    this.animationState = 'closing';
  }

  private close() {
    this.store.dispatch(removeNotification({notification: this.notification}));
  }

  getMessage(): string {
    switch (this.notification.state) {
      case 'error':
        return this.notification.error.message;
      default:
        return this.notification.msg;
    }
  }

}

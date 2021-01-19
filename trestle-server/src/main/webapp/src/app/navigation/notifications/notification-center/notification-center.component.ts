import {Component, OnInit} from '@angular/core';
import {selectNotificationsFromNotifications, TrestleNotification} from "../../../reducers/notification.reducers";
import {Observable} from "rxjs";
import {select, Store} from "@ngrx/store";
import {State} from "../../../reducers";

@Component({
  selector: 'app-notification-center',
  templateUrl: './notification-center.component.html',
  styleUrls: ['./notification-center.component.scss']
})
export class NotificationCenterComponent implements OnInit {

  public notifications: Observable<TrestleNotification[]>;

  constructor(private store: Store<State>) {
    // Not used
  }

  ngOnInit(): void {
    this.notifications = this.store
      .pipe(select(selectNotificationsFromNotifications));
  }
}

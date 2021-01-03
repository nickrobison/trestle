import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {TrestleNotification} from "../../../reducers/notification.reducers";
import {Store} from "@ngrx/store";
import {State} from "../../../reducers";
import {removeNotification} from "../../../actions/notification.actions";

@Component({
  selector: 'app-toast',
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.scss']
})
export class ToastComponent implements OnInit, OnDestroy {

  @Input()
  notification: TrestleNotification;
  private intervalId: number;

  constructor(private store: Store<State>) {
  }

  ngOnInit(): void {
    this.intervalId = setTimeout(() => this.store.dispatch(removeNotification({notification: this.notification})), 5000);
  }

  ngOnDestroy() {
    console.debug("Destroy called!");
    clearTimeout(this.intervalId);
  }

}

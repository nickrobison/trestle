import {Component, OnDestroy, OnInit} from '@angular/core';
import {ToastRef} from "./ToastRef";
import {ToastData} from "./toast-config";

@Component({
  selector: 'app-toast',
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.scss']
})
export class ToastComponent implements OnInit, OnDestroy {

  private intervalId: number;

  constructor(readonly data: ToastData, readonly ref: ToastRef) { }

  ngOnInit(): void {
    // this.intervalId = setTimeout(() => this.close(), 5000);
  }

  ngOnDestroy() {
    // clearTimeout(this.intervalId);
  }

  private close() {
    console.debug("Closing");
    this.ref.close();
  }

}

import {Injectable, Injector} from '@angular/core';
import {Overlay} from "@angular/cdk/overlay";
import {ComponentPortal, PortalInjector} from "@angular/cdk/portal";
import {ToastComponent} from "./toast/toast.component";
import {ToastRef} from "./toast/ToastRef";
import {TrestleError, TrestleMessage, TrestleNotification} from "../../reducers/notification.reducers";
import {ToastData} from "./toast/toast-config";

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  private lastToast: ToastRef | null;

  constructor(private overlay: Overlay, private parentInjector: Injector) {
  }

  public addNotification(notification: TrestleNotification) {
    console.debug("Showing notification");

    if (notification.state === 'error') {
      this.show({
        msg: (notification as TrestleError).error.message,
      });
    } else {
      this.show({
        msg: (notification as TrestleMessage).msg,
      });
    }
  }

  public removeNotification(notification: TrestleNotification) {
    // Not used yet
  }

  show(data: ToastData): ToastRef {
    const strategy = this.overlay
      .position()
      .global()
      .right()
      .top(this.getPosition());

    const overlayRef = this.overlay.create({
      positionStrategy: strategy,
    });

    const toastRef = new ToastRef(overlayRef);
    const injector = NotificationService.getInjector(data, toastRef, this.parentInjector);
    const toastPortal = new ComponentPortal(ToastComponent, null, injector);
    overlayRef.attach(toastPortal);

    this.lastToast = toastRef;

    return toastRef;
  }

  private getPosition(): string {
    const position = this.lastToast ? this.lastToast.getPosition().bottom : 0;
    return position + 'px';
  }

  private static getInjector(data: ToastData, toastRef: ToastRef, parentInjector: Injector) {
    const tokens = new WeakMap();
    tokens.set(ToastData, data);
    tokens.set(ToastRef, toastRef);

    return new PortalInjector(parentInjector, tokens);
  }
}

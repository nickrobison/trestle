import {Injectable, Injector} from '@angular/core';
import {Overlay} from "@angular/cdk/overlay";
import {ComponentPortal, PortalInjector} from "@angular/cdk/portal";
import {NotificationCenterComponent} from "./notification-center/notification-center.component";
import {Store} from "@ngrx/store";
import {State} from "../../reducers";

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor(private overlay: Overlay, private parentInjector: Injector, private store: Store<State>) {
  }

  public createNotificationCenter() {
    const injector = this.getInjector();

    const strategy = this.overlay
      .position()
      .global()
      .right()
      .top("50px");

    const overlayRef = this.overlay.create({
      positionStrategy: strategy,
    });
    const toastPortal = new ComponentPortal(NotificationCenterComponent, null, injector);
    overlayRef.attach(toastPortal);
  }

  private getInjector() {
    const tokens = new WeakMap();
    tokens.set(Store, this.store);
    return new PortalInjector(this.parentInjector, tokens);
  }
}

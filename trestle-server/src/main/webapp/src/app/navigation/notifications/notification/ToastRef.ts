import {OverlayRef} from "@angular/cdk/overlay";

export class ToastRef {
  constructor(private readonly overlay: OverlayRef) {
    console.debug("Here I am!", this.getPosition());
  }

  public getPosition(): DOMRect {
    return this.overlay.overlayElement.getBoundingClientRect();
  }

  public close() {
    this.overlay.detach();
  }
}

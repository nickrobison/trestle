import {ApplicationRef, ComponentFactoryResolver, Injectable, Injector, ViewContainerRef} from "@angular/core";
import {ComponentPortal, DomPortalHost} from "@angular/cdk/portal";
import {LoadingSpinnerComponent} from "./loading-spinner.component";

@Injectable()
export class LoadingSpinnerService {

    private loadingSpinnerPortal: ComponentPortal<LoadingSpinnerComponent>;
    private bodyPortalHost: DomPortalHost;
    private domElement: HTMLElement;

    public constructor(private componentFactoryResolver: ComponentFactoryResolver,
                       private appRef: ApplicationRef,
                       private injector: Injector) {

        this.loadingSpinnerPortal = new ComponentPortal(LoadingSpinnerComponent);
    }

    public setViewContainerRef(ref: ViewContainerRef | HTMLElement): void {
        // const factory = this.componentFactoryResolver
        //     .resolveComponentFactory(LoadingSpinnerComponent);

        // const component = factory.create(ref.parentInjector);

        if (ref instanceof ViewContainerRef) {
            this.domElement = ref.element.nativeElement;
        } else {
            this.domElement = ref;
        }

        console.debug("Setting view ref to:", this.domElement);

        this.bodyPortalHost = new DomPortalHost(
            this.domElement,
            this.componentFactoryResolver,
            this.appRef,
            this.injector
        );
    }

    public reveal(): void {
        if (this.bodyPortalHost == null) {
            throw new Error("Host is empty, cannot add!");
        }
        console.debug("Adding spinner");
        this.bodyPortalHost.attach(this.loadingSpinnerPortal);
    }

    public hide(): void {
        if (this.bodyPortalHost == null) {
            throw new Error("Host is empty, cannot remove!");
        }
        console.debug("Removing spinner");
        this.bodyPortalHost.detach();
    }
}
/**
 * Created by nrobison on 6/11/17.
 */
import {NgModule} from "@angular/core";
import {TrestleMapComponent} from "./map/trestle-map.component";
import { EventBus } from "./eventBus/eventBus.service";

@NgModule({
    declarations: [
        TrestleMapComponent
    ],
    providers: [EventBus],
    exports: [TrestleMapComponent]
})
export class UIModule {}

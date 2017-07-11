/**
 * Created by nrobison on 6/11/17.
 */
import {NgModule} from "@angular/core";
import {TrestleMapComponent} from "./map/trestle-map.component";
import { EventBus } from "./eventBus/eventBus.service";
import { MapValuesPipe } from "./pipes/map-values.pipe";

@NgModule({
    declarations: [
        TrestleMapComponent,
        MapValuesPipe,
    ],
    providers: [EventBus],
    exports: [TrestleMapComponent, MapValuesPipe]
})
export class UIModule {}

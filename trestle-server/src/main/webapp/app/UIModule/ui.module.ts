/**
 * Created by nrobison on 6/11/17.
 */
import {NgModule} from "@angular/core";
import {TrestleMapComponent} from "./map/map.component";

@NgModule({
    declarations: [
        TrestleMapComponent
    ],
    exports: [TrestleMapComponent]
})
export class UIModule {}

/**
 * Created by nrobison on 6/11/17.
 */
import {NgModule} from "@angular/core";
import {TrestleMapComponent} from "./map/trestle-map.component";
import {EventBus} from "./eventBus/eventBus.service";
import {MapValuesPipe} from "./pipes/map-values.pipe";
import {HistoryGraphComponent} from "./history-graph/history-graph.component";
import {EventGraphComponent} from "./event-graph/event-graph.component";
import {SharedModule} from "../SharedModule/shared.module";
import {SearchComponent} from "./search/search.component";
import {MaterialModule} from "../MaterialModule/material.module";
import {CommonModule} from "@angular/common";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";

@NgModule({
    imports: [
        SharedModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        MaterialModule
    ],
    declarations: [
        TrestleMapComponent,
        HistoryGraphComponent,
        EventGraphComponent,
        SearchComponent,
        MapValuesPipe,
    ],
    providers: [EventBus],
    exports: [TrestleMapComponent,
        MapValuesPipe,
        HistoryGraphComponent,
        EventGraphComponent,
        SearchComponent]
})
export class UIModule {
}

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HistoryGraphComponent } from './history-graph/history-graph.component';
import { TrestleMapComponent } from './trestle-map/trestle-map.component';
import { SpatialUnionComponent } from './spatial-union/spatial-union.component';
import {SharedModule} from '../shared/shared.module';
import { EventGraphComponent } from './event-graph/event-graph.component';



@NgModule({
  declarations: [HistoryGraphComponent, TrestleMapComponent, SpatialUnionComponent, EventGraphComponent],
  exports: [
    TrestleMapComponent,
    HistoryGraphComponent,
    SpatialUnionComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ]
})
export class UiModule { }

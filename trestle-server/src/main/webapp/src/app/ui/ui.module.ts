import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {HistoryGraphComponent} from './history-graph/history-graph.component';
import {TrestleMapComponent} from './trestle-map/trestle-map.component';
import {SpatialUnionComponent} from './spatial-union/spatial-union.component';
import {SharedModule} from '../shared/shared.module';
import {EventGraphComponent} from './event-graph/event-graph.component';
import {LoadingSpinnerComponent} from './loading-spinner/loading-spinner.component';
import {LoadingSpinnerService} from './loading-spinner/loading-spinner.service';
import {SearchComponent} from './search/search.component';
import {MaterialModule} from '../material/material.module';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';

@NgModule({
  declarations: [
    HistoryGraphComponent,
    TrestleMapComponent,
    SpatialUnionComponent,
    EventGraphComponent,
    LoadingSpinnerComponent,
    SearchComponent],
  exports: [
    TrestleMapComponent,
    HistoryGraphComponent,
    SpatialUnionComponent,
    SearchComponent
  ],
  providers: [LoadingSpinnerService],
  imports: [
    CommonModule,
    SharedModule,
    MaterialModule,
    ReactiveFormsModule,
    FormsModule
  ]
})
export class UiModule {
}

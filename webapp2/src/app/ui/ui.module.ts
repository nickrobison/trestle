import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HistoryGraphComponent } from './history-graph/history-graph.component';
import { TrestleMapComponent } from './trestle-map/trestle-map.component';



@NgModule({
  declarations: [HistoryGraphComponent, TrestleMapComponent],
  imports: [
    CommonModule
  ]
})
export class UiModule { }

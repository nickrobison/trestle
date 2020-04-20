import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {IndividualService} from './individual/individual.service';
import {DATASET_CACHE, DatasetService} from './dataset/dataset.service';
import {CacheService} from './cache/cache.service';
import {ArraySortPipe} from './pipes/array-sort.pipe';
import {DATASET_CACHE_DI_CONFIG} from './shared.config';
import {MapValuesPipe} from './pipes/map-values.pipe';
import {RoundingPipe} from './pipes/rounding-pipe.pipe';
import {ColorService} from './color/color.service';


@NgModule({
  declarations: [ArraySortPipe, MapValuesPipe, RoundingPipe],
  providers: [
    IndividualService,
    DatasetService,
    CacheService,
    ColorService,
    {
      provide: DATASET_CACHE, useFactory: () => (new CacheService<string, string[]>(DATASET_CACHE_DI_CONFIG))
    }],
  imports: [
    CommonModule
  ],
  exports: [
    MapValuesPipe,
    ArraySortPipe,
    RoundingPipe
  ]
})
export class SharedModule {
}

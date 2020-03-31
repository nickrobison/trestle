import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {IndividualService} from './individual/individual.service';
import {DATASET_CACHE, DatasetService} from './dataset/dataset.service';
import {CacheService} from './cache/cache.service';
import {ArraySortPipePipe} from './pipes/array-sort.pipe';
import {DATASET_CACHE_DI_CONFIG} from './shared.config';



@NgModule({
  declarations: [ArraySortPipePipe],
  providers: [
    IndividualService,
    DatasetService,
    CacheService,
    {
      provide: DATASET_CACHE, useFactory: () => (new CacheService<string, string[]>(DATASET_CACHE_DI_CONFIG))
    }],
  imports: [
    CommonModule
  ]
})
export class SharedModule { }

import { NgModule } from "@angular/core";
import { CacheService } from "./cache/cache.service";
import { IndividualService } from "./individual/individual.service";
import { RoundingPipe } from "./pipes/rounding.pipe";
import { MapValuesPipe } from "./pipes/map-values.pipe";
import { ColorService } from "./color/color.service";
import { DATASET_CACHE, DatasetService } from "./dataset/dataset.service";
import { DATASET_CACHE_DI_CONFIG } from "./shared.config";
import { ArraySortPipe } from "./pipes/array-sort.pipe";

@NgModule({
    declarations: [
        RoundingPipe,
        MapValuesPipe,
        ArraySortPipe],
    providers: [
        CacheService,
        IndividualService,
        ColorService,
        DatasetService,
        {
            provide: DATASET_CACHE, useFactory: () => (new CacheService<string, string[]>(DATASET_CACHE_DI_CONFIG))
        }],
    exports: [
        RoundingPipe,
        MapValuesPipe,
        ArraySortPipe]
})

export class SharedModule {
}

import {NgModule} from "@angular/core";
import {CacheService} from "./cache/cache.service";
import {IndividualService} from "./individual/individual.service";
import {RoundingPipe} from "./pipes/rounding.pipe";
import {MapValuesPipe} from "./pipes/map-values.pipe";

@NgModule({
    declarations: [RoundingPipe,
        MapValuesPipe],
    providers: [CacheService, IndividualService],
    exports: [RoundingPipe,
        MapValuesPipe]
})

export class SharedModule {
}

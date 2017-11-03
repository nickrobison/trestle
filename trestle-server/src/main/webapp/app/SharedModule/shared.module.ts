import {NgModule} from "@angular/core";
import {CacheService} from "./cache/cache.service";
import {IndividualService} from "./individual/individual.service";
import {RoundingPipe} from "./pipes/rounding.pipe";

@NgModule({
    declarations: [RoundingPipe],
    providers: [CacheService, IndividualService],
    exports: [RoundingPipe]
})

export class SharedModule {}

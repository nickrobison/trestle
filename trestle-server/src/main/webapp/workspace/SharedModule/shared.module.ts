import { NgModule } from "@angular/core";
import { CacheService } from "./cache/cache.service";
import { IndividualService } from "./individual/individual.service";
import { RoundingPipe } from "./pipes/rounding.pipe";
import { MapValuesPipe } from "./pipes/map-values.pipe";
import { ColorService } from "./color/color.service";

@NgModule({
    declarations: [
        RoundingPipe,
        MapValuesPipe],
    providers: [
        CacheService,
        IndividualService,
        ColorService],
    exports: [
        RoundingPipe,
        MapValuesPipe]
})

export class SharedModule {
}

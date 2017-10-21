import {NgModule} from "@angular/core";
import {CacheService} from "./cache/cache.service";
import {IndividualService} from "./individual/individual.service";

@NgModule({
    providers: [CacheService, IndividualService]
})

export class SharedModule {}

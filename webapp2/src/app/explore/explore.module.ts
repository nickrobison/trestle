import {NgModule, NO_ERRORS_SCHEMA} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ViewerComponent} from './viewer/viewer.component';
import {UserModule} from '../user/user.module';
import {NavigationModule} from '../navigation/navigation.module';
import {HttpClientModule} from '@angular/common/http';
import {UiModule} from '../ui/ui.module';
import {ExporterComponent} from './exporter/exporter.component';
import {RouterModule} from '@angular/router';
import {ExploreRoutes} from './explore.routing';
import {MapService} from './viewer/map.service';
import {INDIVIDUAL_CACHE, IndividualService} from '../shared/individual/individual.service';
import {ExporterService} from './exporter/exporter.service';
import {CacheService} from '../shared/cache/cache.service';
import {TrestleIndividual} from '../shared/individual/TrestleIndividual/trestle-individual';
import {INDIVIDUAL_CACHE_DI_CONFIG} from './explore.config';
import {CACHE_SERVICE_CONFIG} from '../shared/cache/cache.service.config';
import {SharedModule} from '../shared/shared.module';


@NgModule({
  declarations: [ViewerComponent, ExporterComponent],
  imports: [
    CommonModule,
    UserModule,
    NavigationModule,
    HttpClientModule,
    SharedModule,
    UiModule,
    RouterModule.forChild(ExploreRoutes)
  ],
  providers: [
    ExporterService,
    IndividualService,
    MapService,
    // {
    //   provide: COLOR_SERVICE_CONFIG, useValue: COLOR_DI_CONFIG
    // },
    {
      provide: CACHE_SERVICE_CONFIG, useValue: INDIVIDUAL_CACHE_DI_CONFIG
    },
    {
      provide: INDIVIDUAL_CACHE, useFactory: () => (new CacheService<string, TrestleIndividual>(INDIVIDUAL_CACHE_DI_CONFIG))
    }
  ],
  schemas: [NO_ERRORS_SCHEMA]
})
export class ExploreModule {
}

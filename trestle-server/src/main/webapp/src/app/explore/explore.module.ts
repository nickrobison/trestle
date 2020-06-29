import {NgModule, NO_ERRORS_SCHEMA} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ViewerComponent} from './viewer/viewer.component';
import {UserModule} from '../user/user.module';
import {NavigationModule} from '../navigation/navigation.module';
import {UiModule} from '../ui/ui.module';
import {ExporterComponent} from './exporter/exporter.component';
import {RouterModule} from '@angular/router';
import {ExploreRoutes} from './explore.routing';
import {MapService} from './viewer/map.service';
import {INDIVIDUAL_CACHE, IndividualService} from '../shared/individual/individual.service';
import {ExporterService} from './exporter/exporter.service';
import {CacheService} from '../shared/cache/cache.service';
import {TrestleIndividual} from '../shared/individual/TrestleIndividual/trestle-individual';
import {COLOR_DI_CONFIG, INDIVIDUAL_CACHE_DI_CONFIG} from './explore.config';
import {CACHE_SERVICE_CONFIG} from '../shared/cache/cache.service.config';
import {SharedModule} from '../shared/shared.module';
import {MaterialModule} from '../material/material.module';
import {QueryComponent} from './query/query.component';
import {CodeMirrorComponent} from './query/codemirror/codemirror.component';
import {QueryService} from './query/query.service';
import {QueryViewerComponent} from './query/query-viewer/query-viewer.component';
import {VisualizeComponent} from './visualize/visualize.component';
import {IndividualGraphComponent} from './visualize/individual-graph/individual-graph.component';
import {VisualizeDetailsComponent} from './visualize/visualize-details/visualize-details.component';
import {IndividualValueDialog} from './visualize/individual-value.dialog';
import {CompareComponent} from './compare/compare.component';
import {COLOR_SERVICE_CONFIG} from '../shared/color/color-service.config';
import {AggregateComponent} from './aggregate/aggregate.component';
import {AggregationService} from './aggregate/aggregation.service';
import {FlexModule} from '@angular/flex-layout';


@NgModule({
  declarations: [ViewerComponent,
    ExporterComponent,
    QueryComponent,
    CodeMirrorComponent,
    QueryViewerComponent,
    VisualizeComponent,
    IndividualGraphComponent,
    VisualizeDetailsComponent,
    IndividualValueDialog,
    CompareComponent,
    AggregateComponent],
  imports: [
    CommonModule,
    UserModule,
    MaterialModule,
    NavigationModule,
    SharedModule,
    UiModule,
    RouterModule.forChild(ExploreRoutes),
    FlexModule
  ],
  providers: [
    AggregationService,
    ExporterService,
    IndividualService,
    MapService,
    QueryService,
    {
      provide: COLOR_SERVICE_CONFIG, useValue: COLOR_DI_CONFIG
    },
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

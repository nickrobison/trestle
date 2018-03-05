/**
 * Created by nrobison on 5/12/17.
 */
import { NgModule } from "@angular/core";
import { UserModule } from "../UserModule/user.module";
import { VisualizeComponent } from "./visualize/visualize.component";
import { IndividualGraphComponent } from "./visualize/individual-graph.component";
import { IndividualValueDialog } from "./visualize/individual-value.dialog";
import { IndividualService } from "../SharedModule/individual/individual.service";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { QueryViewer } from "./query/query-viewer/query-viewer.component";
import { QueryComponent } from "./query/query.component";
import { CodeMirrorComponent } from "./query/codemirror/codemirror.component";
import { QueryService } from "./query/query.service";
import { UIModule } from "../UIModule/ui.module";
import { RouterModule } from "@angular/router";
import { ExploreRoutes } from "./explore.routing";
import { DatsetViewerComponent } from "./viewer/viewer.component";
import { MapService } from "./viewer/map.service";
import { MaterialModule } from "../MaterialModule/material.module";
import { SharedModule } from "../SharedModule/shared.module";
import { CompareComponent } from "./compare/compare.component";
import { VisualizeDetailsComponent } from "./visualize/details/visualize-details.component";
import { ExporterComponent } from "./exporter/exporter.component";
import { ExporterService } from "./exporter/exporter.service";
import { SpatialUnionComponent } from "./spatial-union/spatial-union.component";
import { COLOR_SERVICE_CONFIG } from "../SharedModule/color/color-service.config";
import { CACHE_DI_CONFIG, COLOR_DI_CONFIG } from "./explore.config";
import { CACHE_SERVICE_CONFIG } from "../SharedModule/cache/cache.service.config";

@NgModule({
    imports: [
        UserModule,
        FormsModule,
        ReactiveFormsModule,
        MaterialModule,
        CommonModule,
        UIModule,
        SharedModule,
        RouterModule.forChild(ExploreRoutes)
    ],
    declarations: [
        VisualizeComponent,
        IndividualGraphComponent,
        IndividualValueDialog,
        CodeMirrorComponent,
        QueryComponent,
        QueryViewer,
        DatsetViewerComponent,
        CompareComponent,
        VisualizeDetailsComponent,
        ExporterComponent,
        SpatialUnionComponent
    ],
    providers: [IndividualService,
        QueryService,
        MapService,
        ExporterService,
        {
            provide: COLOR_SERVICE_CONFIG, useValue: COLOR_DI_CONFIG
        },
        {
            provide: CACHE_SERVICE_CONFIG, useValue: CACHE_DI_CONFIG
        }],
    entryComponents: [IndividualValueDialog]
})
export class ExploreModule {
}

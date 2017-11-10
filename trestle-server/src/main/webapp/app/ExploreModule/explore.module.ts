/**
 * Created by nrobison on 5/12/17.
 */
import {NgModule} from "@angular/core";
import {UserModule} from "../UserModule/user.module";
import {VisualizeComponent} from "./visualize/visualize.component";
import {IndividualGraphComponent} from "./visualize/individual-graph.component";
import {IndividualValueDialog} from "./visualize/individual-value.dialog";
import {IndividualService} from "../SharedModule/individual/individual.service";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {CommonModule} from "@angular/common";
import {QueryViewer} from "./query/query-viewer/query-viewer.component";
import {QueryComponent} from "./query/query.component";
import {CodeMirrorComponent} from "./query/codemirror/codemirror.component";
import {QueryService} from "./query/query.service";
import {UIModule} from "../UIModule/ui.module";
import {RouterModule} from "@angular/router";
import {ExploreRoutes} from "./explore.routing";
import { DatsetViewerComponent } from "./viewer/viewer.component";
import { MapService } from "./viewer/map.service";
import { ObjectEventGraphComponent } from "./object-event-graph/ObjectEventGraph";
import { MaterialModule } from "../MaterialModule/material.module";
import {SharedModule} from "../SharedModule/shared.module";
import {CompareComponent} from "./compare/compare.component";
import {VisualizeDetailsComponent} from "./visualize/details/visualize-details.component";
import {ExporterComponent} from "./exporter/exporter.component";
import {ExporterService} from "./exporter/exporter.service";

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
        ObjectEventGraphComponent,
        CompareComponent,
        VisualizeDetailsComponent,
        ExporterComponent
    ],
    providers: [IndividualService, QueryService, MapService, ExporterService],
    entryComponents: [IndividualValueDialog]
})
export class ExploreModule {}

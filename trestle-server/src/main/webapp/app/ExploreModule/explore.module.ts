/**
 * Created by nrobison on 5/12/17.
 */
import {NgModule} from "@angular/core";
import {UserModule} from "../UserModule/user.module";
import {VisualizeComponent} from "./visualize/visualize.component";
import {IndividualGraph} from "./visualize/individual-graph.component";
import {IndividualValueDialog} from "./visualize/individual-value.dialog";
import {VisualizeService} from "./visualize/visualize.service";
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

@NgModule({
    imports: [
        UserModule,
        FormsModule,
        ReactiveFormsModule,
        MaterialModule,
        CommonModule,
        UIModule,
        RouterModule.forChild(ExploreRoutes)
    ],
    declarations: [
        VisualizeComponent,
        IndividualGraph,
        IndividualValueDialog,
        CodeMirrorComponent,
        QueryComponent,
        QueryViewer,
        DatsetViewerComponent,
        ObjectEventGraphComponent
    ],
    providers: [VisualizeService, QueryService, MapService],
    entryComponents: [IndividualValueDialog]
})
export class ExploreModule {}

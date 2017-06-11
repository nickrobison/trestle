/**
 * Created by nrobison on 5/12/17.
 */
import {NgModule} from "@angular/core";
import {UserModule} from "../UserModule/user.module";
import {VisualizeComponent} from "./visualize/visualize.component";
import {IndividualGraph} from "./visualize/individual-graph.component";
import {FactHistoryGraph} from "./visualize/fact-graph.component";
import {IndividualValueDialog} from "./visualize/individual-value.dialog";
import {VisualizeService} from "./visualize/visualize.service";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MaterialModule} from "@angular/material";
import {CommonModule} from "@angular/common";
import {QueryViewer} from "./query/query-viewer/query-viewer.component";
import {QueryComponent} from "./query/query.component";
import {CodeMirrorComponent} from "./query/codemirror/codemirror.component";
import {QueryService} from "./query/query.service";
import {UIModule} from "../UIModule/ui.module";

@NgModule({
    imports: [
        UserModule,
        FormsModule,
        ReactiveFormsModule,
        MaterialModule,
        CommonModule,
        UIModule
    ],
    declarations: [
        VisualizeComponent,
        IndividualGraph,
        FactHistoryGraph,
        IndividualValueDialog,
        CodeMirrorComponent,
        QueryComponent,
        QueryViewer
    ],
    providers: [VisualizeService, QueryService],
    entryComponents: [IndividualValueDialog]
})
export class ExploreModule {}

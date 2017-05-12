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

@NgModule({
    imports: [
        UserModule,
        FormsModule,
        ReactiveFormsModule,
        MaterialModule,
        CommonModule
    ],
    declarations: [
        VisualizeComponent,
        IndividualGraph,
        FactHistoryGraph,
        IndividualValueDialog
    ],
    providers: [VisualizeService],
    entryComponents: [IndividualValueDialog]
})
export class ExploreModule {}

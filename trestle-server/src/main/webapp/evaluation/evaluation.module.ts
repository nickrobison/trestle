import { ErrorHandler, NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { MaterialModule } from "../workspace/MaterialModule/material.module";
import { HttpClientModule } from "@angular/common/http";
import { EvaluationComponent } from "./evaluation.component";
import { IntroductionComponent } from "./introduction/introduction.component";
import { RouterModule } from "@angular/router";
import { EvaluationRoutes } from "./evaluation.routes";
import { DemographicsComponent } from "./introduction/demographics/demographics.component";
import { EvaluationService } from "./eval-service/evaluation.service";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { ExperimentComponent } from "./experiment/experiment.component";
import { UIModule } from "../workspace/UIModule/ui.module";
import "../rxjs-operators";
import { CommonModule } from "@angular/common";
import { SharedModule } from "../workspace/SharedModule/shared.module";
import { FlexLayoutModule } from "@angular/flex-layout";
import { CounterPipe, SelectionTableComponent } from "./experiment/selection-table/selection-table.component";
import { COLOR_SERVICE_CONFIG } from "../workspace/SharedModule/color/color-service.config";
import { COLOR_EVAL_CONFIG } from "./evaluation.config";
import { ErrorService } from "../workspace/SharedModule/errors/error.service";
import { ConclusionComponent } from "./conclusion/conclusion.component";

@NgModule({
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        CommonModule,
        FlexLayoutModule,
        FormsModule,
        HttpClientModule,
        MaterialModule,
        ReactiveFormsModule,
        RouterModule.forRoot(EvaluationRoutes),
        SharedModule,
        UIModule
    ],
    declarations: [
        EvaluationComponent,
        IntroductionComponent,
        DemographicsComponent,
        ExperimentComponent,
        SelectionTableComponent,
        ConclusionComponent,
        CounterPipe
    ],
    providers: [
        EvaluationService,
        {
            provide: COLOR_SERVICE_CONFIG, useValue: COLOR_EVAL_CONFIG
        },
        {
            provide: ErrorHandler,
            useClass: ErrorService
        }],
    bootstrap: [EvaluationComponent]
})
export class EvaluationModule {
}

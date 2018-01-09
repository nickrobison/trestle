import { NgModule } from "@angular/core";
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

@NgModule({
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        CommonModule,
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
        ExperimentComponent
    ],
    providers: [
        EvaluationService
    ],
    bootstrap: [EvaluationComponent]
})
export class EvaluationModule {
}

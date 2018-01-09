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

@NgModule({
    imports: [
        RouterModule.forRoot(EvaluationRoutes),
        HttpClientModule,
        BrowserModule,
        BrowserAnimationsModule,
        CommonModule,
        MaterialModule,
        FormsModule,
        ReactiveFormsModule,
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

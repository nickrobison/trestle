import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { MaterialModule } from "../workspace/MaterialModule/material.module";
import { HttpClientModule } from "@angular/common/http";
import { EvaluationComponent } from "./evaluation.component";
import { IntroductionComponent } from "./introduction/introduction.component";
import { RouterModule } from "@angular/router";
import { EvaluationRoutes } from "./evaluation.routes";

@NgModule({
    imports: [
        RouterModule.forRoot(EvaluationRoutes),
        HttpClientModule,
        BrowserModule,
        BrowserAnimationsModule,
        MaterialModule
    ],
    declarations: [
        EvaluationComponent,
        IntroductionComponent
    ],
    bootstrap: [EvaluationComponent]
})
export class EvaluationModule { }

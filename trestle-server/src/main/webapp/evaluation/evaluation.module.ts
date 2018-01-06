import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { MaterialModule } from "../workspace/MaterialModule/material.module";
import { HttpClientModule } from "@angular/common/http";
import { EvaluationComponent } from "./evaluation.component";

@NgModule({
    imports: [
        HttpClientModule,
        BrowserModule,
        BrowserAnimationsModule,
        MaterialModule
    ],
    declarations: [EvaluationComponent],
    bootstrap: [EvaluationComponent]
})
export class EvaluationModule { }

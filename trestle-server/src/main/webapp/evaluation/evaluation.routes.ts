import { Route } from "@angular/router";
import { IntroductionComponent } from "./introduction/introduction.component";
import { DemographicsComponent } from "./introduction/demographics/demographics.component";
import { ExperimentComponent } from "./experiment/experiment.component";
import { ConclusionComponent } from "./conclusion/conclusion.component";

export const EvaluationRoutes: Route[] = [
    {path: "", redirectTo: "introduction", pathMatch: "full"},
    {path: "introduction", component: IntroductionComponent},
    {path: "demographics", component: DemographicsComponent},
    {path: "experiment", component: ExperimentComponent},
    {path: "conclusion", component: ConclusionComponent}
];

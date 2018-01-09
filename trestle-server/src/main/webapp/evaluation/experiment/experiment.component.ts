import { AfterViewInit, Component } from "@angular/core";
import { EvaluationService } from "../eval-service/evaluation.service";

@Component({
    selector: "experiment",
    templateUrl: "./experiment.component.html",
    styleUrls: ["./experiment.component.css"]
})
export class ExperimentComponent implements AfterViewInit {

    public experimentValue: number;
    public answered: string | undefined;

    public constructor(private es: EvaluationService) {
        // Start with 10, because the bar to goes to 100.
        this.experimentValue = 1;
        // this.progressValue = 10;
    }

    public ngAfterViewInit(): void {
        this.loadNextMatch();
    }

    public next(): void {
        this.answered = undefined;
        this.experimentValue += 1;
        this.loadNextMatch();
    }

    public loadNextMatch(): void {
        this.es.loadExperiment(this.experimentValue)
            .subscribe((experiment) => {
                console.debug("Overlay?", this.es.isOverlay(experiment.state));
            });
    }
}

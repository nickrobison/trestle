import { AfterViewInit, Component } from "@angular/core";
import { EvaluationService } from "../eval-service/evaluation.service";
import { BehaviorSubject } from "rxjs/BehaviorSubject";
import { MapSource } from "../../workspace/UIModule/map/trestle-map.component";

@Component({
    selector: "experiment",
    templateUrl: "./experiment.component.html",
    styleUrls: ["./experiment.component.css"]
})
export class ExperimentComponent implements AfterViewInit {

    public experimentValue: number;
    public answered: string | undefined;
    public dataChanges: BehaviorSubject<MapSource | undefined>;

    public constructor(private es: EvaluationService) {
        this.experimentValue = 1;

        this.dataChanges = new BehaviorSubject(undefined);
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
                console.debug("has it:", experiment);
                console.debug("Overlay?", this.es.isOverlay(experiment.state));
            });
    }
}

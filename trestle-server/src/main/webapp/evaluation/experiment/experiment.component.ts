import { Component } from "@angular/core";

@Component({
    selector: "experiment",
    templateUrl: "./experiment.component.html",
    styleUrls: ["./experiment.component.css"]
})
export class ExperimentComponent {

    public progressValue: number;

    public constructor() {
        // Start with 10, because the bar to goes to 100.
        this.progressValue = 10;
    }

    public loadNextMatch(): void {

        this.progressValue += 10;
    }
}

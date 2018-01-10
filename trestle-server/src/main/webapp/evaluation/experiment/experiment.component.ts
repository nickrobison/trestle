import { AfterViewInit, Component } from "@angular/core";
import { EvaluationService } from "../eval-service/evaluation.service";
import { BehaviorSubject } from "rxjs/BehaviorSubject";
import { MapSource } from "../../workspace/UIModule/map/trestle-map.component";
import { TrestleTemporal } from "../../workspace/SharedModule/individual/TrestleIndividual/trestle-temporal";

@Component({
    selector: "experiment",
    templateUrl: "./experiment.component.html",
    styleUrls: ["./experiment.component.css"]
})
export class ExperimentComponent implements AfterViewInit {

    public experimentValue: number;
    public answered: string | undefined;
    public dataChanges: BehaviorSubject<MapSource | undefined>;

    private maxHeight: number;

    public constructor(private es: EvaluationService) {
        this.experimentValue = 1;

        this.dataChanges = new BehaviorSubject(undefined);
        this.maxHeight = 2016;
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
                experiment.individuals.forEach((individual) => {

                    const height = this.getHeight(individual.getTemporal());
                    const baseHeight = ExperimentComponent.getBase(individual.getTemporal());
                    this.dataChanges.next({
                        id: individual.getID(),
                        data: {
                            type: "Feature",
                            geometry: individual.getSpatialValue(),
                            id: individual.getFilteredID(),
                            properties: individual.getFactValues()
                        },
                        extrude: {
                            id: individual.getID() + "-extrude",
                            type: "fill-extrusion",
                            source: individual.getID(),
                            paint: {
                                "fill-extrusion-color": "blue",
                                "fill-extrusion-height": height,
                                "fill-extrusion-base": baseHeight,
                                "fill-extrusion-opacity": 0.7
                            }
                        },
                        labelField: "adm2_name"
                    });
                });
            });
    }

    private getHeight(temporal: TrestleTemporal): number {
        const to = temporal.getTo();
        if (to === undefined) {
            return this.maxHeight;
        } else {
            return to.get("year");
        }
    }

    private static getBase(temporal: TrestleTemporal): number {
        return temporal.getFrom().get("year");
    }
}

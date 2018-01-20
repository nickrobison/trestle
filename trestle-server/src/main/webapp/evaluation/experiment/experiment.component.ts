import { AfterViewInit, Component, ViewChild } from "@angular/core";
import { EvaluationService } from "../eval-service/evaluation.service";
import { BehaviorSubject } from "rxjs/BehaviorSubject";
import { MapSource, TrestleMapComponent } from "../../workspace/UIModule/map/trestle-map.component";
import { TrestleTemporal } from "../../workspace/SharedModule/individual/TrestleIndividual/trestle-temporal";
import { SelectionTableComponent } from "./selection-table/selection-table.component";

@Component({
    selector: "experiment",
    templateUrl: "./experiment.component.html",
    styleUrls: ["./experiment.component.css"]
})
export class ExperimentComponent implements AfterViewInit {

    public experimentValue: number;
    public answered: boolean | undefined;
    public dataChanges: BehaviorSubject<MapSource | undefined>;
    public tableData: string[];
    @ViewChild(TrestleMapComponent)
    public map: TrestleMapComponent;
    @ViewChild(SelectionTableComponent)
    public selectionTable: SelectionTableComponent;
    public minimalSelection: boolean;

    private maxHeight: number;

    public constructor(private es: EvaluationService) {
        this.experimentValue = 1;

        this.dataChanges = new BehaviorSubject(undefined);
        this.maxHeight = 2016;

        this.minimalSelection = false;
    }

    public ngAfterViewInit(): void {
        this.loadNextMatch();
    }

    public next(): void {
        console.debug("Selected:", this.selectionTable.getSelectedRows());
        this.answered = undefined;
        this.experimentValue += 1;
        this.loadNextMatch();
    }

    public loadNextMatch(): void {
        this.es.loadExperiment(this.experimentValue)
            .subscribe((experiment) => {
                console.debug("has it:", experiment);
                console.debug("Overlay?", this.es.isOverlay(experiment.state));

                // Add to table
                this.tableData = experiment.unionOf;

                experiment.individuals.forEach((individual, idx) => {
                    console.debug("Individual:", individual, idx);

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
                        labelValue: idx.toString()
                        // labelField: "adm2_name"
                    });
                });
            });
    }

    public minimalSelectionHandler(): void {
        this.minimalSelection = true;
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

import { AfterViewInit, Component, OnInit, ViewChild } from "@angular/core";
import { EvaluationService } from "../eval-service/evaluation.service";
import { MapSource, TrestleMapComponent } from "../../workspace/UIModule/map/trestle-map.component";
import { TrestleTemporal } from "../../workspace/SharedModule/individual/TrestleIndividual/trestle-temporal";
import { SelectionTableComponent } from "./selection-table/selection-table.component";
import { ReplaySubject } from "rxjs/ReplaySubject";
import { MatSliderChange } from "@angular/material";

@Component({
    selector: "experiment",
    templateUrl: "./experiment.component.html",
    styleUrls: ["./experiment.component.css"]
})
export class ExperimentComponent implements OnInit, AfterViewInit {

    public experimentValue: number;
    public answered: boolean | undefined;
    public dataChanges: ReplaySubject<MapSource>;
    public tableData: string[];
    @ViewChild(TrestleMapComponent)
    public map: TrestleMapComponent;
    @ViewChild(SelectionTableComponent)
    public selectionTable: SelectionTableComponent;
    public minimalSelection: boolean;
    public mapVisible: "visible" | "hidden";
    public currentSliderValue: number;

    private maxHeight: number;
    private currentHeight: number;
    private oldSliderValue: number;
    private baseIndividualID: string;

    public constructor(private es: EvaluationService) {
        this.experimentValue = 1;
        this.dataChanges = new ReplaySubject<MapSource>(50);
        this.maxHeight = 2016;
        this.minimalSelection = false;
    }

    public ngOnInit(): void {
        this.currentSliderValue = 0;
        this.oldSliderValue = 0;
        this.currentHeight = 3001;
        this.mapVisible = "hidden";
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

    public loadNextMatch() {
        // Make the map invisible and remove everything
        this.mapVisible = "hidden";
        this.map.clearMap();

        // Reset the slider values
        this.currentSliderValue = 0;
        this.oldSliderValue = 0;
        this.currentHeight = 3001;

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
                    const filteredID = individual.getFilteredID();
                    const isBase = ExperimentComponent.isBaseIndividual(experiment.union, filteredID);
                    if (isBase) {
                        this.baseIndividualID = filteredID;
                    }
                    this.dataChanges.next({
                        id: filteredID,
                        data: {
                            type: "Feature",
                            geometry: individual.getSpatialValue(),
                            id: filteredID,
                            properties: individual.getFactValues()
                        },
                        extrude: {
                            id: filteredID + "-extrude",
                            type: "fill-extrusion",
                            source: filteredID,
                            paint: {
                                // If we're the base individual, make us red and put us on top of everything else
                                "fill-extrusion-color": isBase ? "red" : "blue",
                                "fill-extrusion-height": isBase ? height + 3001 : height,
                                "fill-extrusion-base": isBase ? baseHeight + 3001 : baseHeight,
                                "fill-extrusion-opacity": isBase ? 1.0 : 0.7
                            }
                        },
                        labelValue: idx.toString()
                    });
                });
                //    Sleep a couple of seconds and then set the map visible again
                //    This is really gross, but who cares?
                this.sleep(4000)
                    .then(() => {
                        this.mapVisible = "visible";
                    });
            });
    }

    public minimalSelectionHandler(): void {
        this.minimalSelection = true;
    }

    public sliderUpdate(event: MatSliderChange): void {
        if (event.value) {
            console.debug("Changed:", event);
            const newOffset = (event.value - this.oldSliderValue) * 50;
            this.map.change3DOffset(this.currentHeight,
                newOffset,
                this.baseIndividualID);
            this.oldSliderValue = event.value;
            this.currentHeight = this.currentHeight + newOffset;
        }
    }

    private getHeight(temporal: TrestleTemporal): number {
        const to = temporal.getTo();
        if (to === undefined) {
            return this.maxHeight;
        } else {
            return to.get("year");
        }
    }

    private sleep(ms: number) {
        return new Promise((resolve) => setTimeout(resolve, ms));
    }

    public static isBaseIndividual(baseIndividualID: string, individualID: string): boolean {
        console.debug("Base:", baseIndividualID, "Individual:", individualID, "Match?:", individualID === baseIndividualID);
        // Strip off GAUL
        return individualID === baseIndividualID;
    }

    private static getBase(temporal: TrestleTemporal): number {
        return temporal.getFrom().get("year");
    }
}

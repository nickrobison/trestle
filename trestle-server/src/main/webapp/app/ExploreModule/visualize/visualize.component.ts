/**
 * Created by nrobison on 3/7/17.
 */
import { Component, OnInit, ViewContainerRef, ViewEncapsulation } from "@angular/core";
import { FormControl } from "@angular/forms";
import { Observable } from "rxjs";
import { IndividualValueDialog } from "./individual-value.dialog";
import * as moment from "moment";
import { ITrestleMapSource } from "../../UIModule/map/trestle-map.component";
import { MatDialog, MatDialogConfig, MatDialogRef } from "@angular/material";
import { IIndividualHistory } from "../../UIModule/history-graph/history-graph.component";
import { TrestleIndividual } from "../../SharedModule/individual/TrestleIndividual/trestle-individual";
import { IndividualService } from "../../SharedModule/individual/individual.service";
import { TrestleFact } from "../../SharedModule/individual/TrestleIndividual/trestle-fact";

@Component({
    selector: "visualize",
    templateUrl: "./visualize.component.html",
    styleUrls: ["./visualize.component.css"],
    encapsulation: ViewEncapsulation.None
})

export class VisualizeComponent implements OnInit {
    public individualName = new FormControl();
    public options: Observable<string[]>;
    public individual: TrestleIndividual;
    public mapIndividual: ITrestleMapSource;
    public individualFactHistory: IIndividualHistory;
    public minTime: moment.Moment;
    public maxTime: moment.Moment;
    private dialogRef: MatDialogRef<IndividualValueDialog> | null;

    constructor(private is: IndividualService,
                private dialog: MatDialog,
                private viewContainerRef: ViewContainerRef) {
    }

    public ngOnInit(): void {
        this.minTime = moment().year(2011).startOf("year");
        this.maxTime = moment().year(2016).endOf("year");
        this.options = this.individualName
            .valueChanges
            .debounceTime(400)
            .distinctUntilChanged()
            .switchMap((name) => this.is.searchForIndividual(name));
    }

    public onSubmit() {
        console.debug("Submitted", this.individualName.value);
        this.is.getTrestleIndividual(this.individualName.value)
            .subscribe((results: TrestleIndividual) => {
                console.debug("has individual", results);
                this.individual = results;

                // Build fact history
                this.individualFactHistory = {
                    entities: results
                        .getFacts()
                        .filter((fact) => fact.getDatabaseTemporal().isContinuing())
                        .map((fact) => {
                            return {
                                label: fact.getName(),
                                start: fact.getValidTemporal().getFromAsDate(),
                                end: fact.getValidTemporal().getToAsDate(),
                                value: fact.getValue()
                            };
                        })
                };
                this.mapIndividual = {
                    id: results.getID(),
                    data: {
                        type: "FeatureCollection",
                        features: [
                            {
                                type: "Feature",
                                geometry: results.getSpatialValue(),
                                id: results.getID(),
                                properties: results.getFactValues()
                            }
                        ]
                    }
                };
            });
    }

    public openValueModal(fact: TrestleFact): void {
        const config = new MatDialogConfig();
        config.viewContainerRef = this.viewContainerRef;
        this.dialogRef = this.dialog.open(IndividualValueDialog, config);
        this.dialogRef.componentInstance.name = fact.getName();
        this.dialogRef.componentInstance.value = fact.getValue();
        this.dialogRef.afterClosed().subscribe(() => this.dialogRef = null);
    }

    public displayFn(individualName: string): string {
        const strings = individualName.split("#");
        return strings[1];
    }

    public selectedOption(value: any) {
        console.debug("Clicked", value);
    }
}

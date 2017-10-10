/**
 * Created by nrobison on 3/7/17.
 */
import { Component, OnInit, ViewContainerRef, ViewEncapsulation } from "@angular/core";
import { VisualizeService, TrestleIndividual, TrestleFact } from "./visualize.service";
import { FormControl } from "@angular/forms";
import { Observable } from "rxjs";
import { IndividualValueDialog } from "./individual-value.dialog";
import { Moment } from "moment";
import moment = require("moment");
import { ITrestleMapSource } from "../../UIModule/map/trestle-map.component";
import { MatDialog, MatDialogConfig, MatDialogRef } from "@angular/material";
import { IIndividualHistory } from "../../UIModule/history-graph/history-graph.component";

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
    public minTime: Moment;
    public maxTime: Moment;
    private dialogRef: MatDialogRef<IndividualValueDialog>;

    constructor(private vs: VisualizeService,
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
            .switchMap((name) => this.vs.searchForIndividual(name));
    }

    public onSubmit() {
        console.debug("Submitted", this.individualName.value);
        this.vs.getIndividualAttributes(this.individualName.value)
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
                                start: fact.getValidTemporal().getFrom().toDate(),
                                end: fact.getValidTemporal().getTo().toDate(),
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
                }
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

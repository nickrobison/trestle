/**
 * Created by nrobison on 3/7/17.
 */
import {Component, OnInit, ViewContainerRef, ViewEncapsulation} from "@angular/core";
import {VisualizeService, ITrestleFact, TrestleIndividual, TrestleFact} from "./visualize.service";
import {FormControl} from "@angular/forms";
import {Observable} from "rxjs";
import {MdDialog, MdDialogConfig, MdDialogRef} from "@angular/material";
import {IndividualValueDialog} from "./individual-value.dialog";
import {Moment} from "moment";
import moment = require("moment");
import {ITrestleMapSource} from "../../UIModule/map/trestle-map.component";

@Component({
    selector: "visualize",
    templateUrl: "./visualize.component.html",
    styleUrls: ["./visualize.component.css"],
    encapsulation: ViewEncapsulation.None
})

export class VisualizeComponent implements OnInit {
    individualName = new FormControl();
    options: Observable<Array<string>>;
    individual: TrestleIndividual;
    mapIndividual: ITrestleMapSource;
    minTime: Moment;
    maxTime: Moment;
    private dialogRef: MdDialogRef<IndividualValueDialog>;

    constructor(private vs: VisualizeService, private dialog: MdDialog, private viewContainerRef: ViewContainerRef) {
    }

    ngOnInit(): void {
        this.minTime = moment().year(2011).startOf("year");
        this.maxTime = moment().year(2016).endOf("year");
        this.options = this.individualName
            .valueChanges
            .debounceTime(400)
            .distinctUntilChanged()
            .switchMap(name => this.vs.searchForIndividual(name));
    }

    onSubmit() {
        console.debug("Submitted", this.individualName.value);
        this.vs.getIndividualAttributes(this.individualName.value)
            .subscribe((results: TrestleIndividual) => {
                console.debug("has individual", results);
                this.individual = results;
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

    openValueModal(fact: TrestleFact): void {
        let config = new MdDialogConfig();
        config.viewContainerRef = this.viewContainerRef;
        this.dialogRef = this.dialog.open(IndividualValueDialog, config);
        this.dialogRef.componentInstance.name = fact.getName();
        this.dialogRef.componentInstance.value = fact.getValue();
        this.dialogRef.afterClosed().subscribe(() => this.dialogRef = null);
    }

    displayFn(individualName: string): string {
        let strings = individualName.split("#");
        return strings[1];
    }
    
    selectedOption(value: any) {
        console.debug("Clicked", value);
    }
}
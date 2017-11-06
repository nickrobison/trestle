import {Component, OnDestroy, OnInit, ViewContainerRef} from "@angular/core";
import {IndividualService} from "../../../SharedModule/individual/individual.service";
import {MatDialog, MatDialogConfig, MatDialogRef} from "@angular/material";
import {ActivatedRoute} from "@angular/router";
import {Subscription} from "rxjs/Subscription";
import {TrestleIndividual} from "../../../SharedModule/individual/TrestleIndividual/trestle-individual";
import {MapSource} from "../../../UIModule/map/trestle-map.component";
import {Subject} from "rxjs/Subject";
import {IIndividualHistory} from "../../../UIModule/history-graph/history-graph.component";
import * as moment from "moment";
import {TrestleFact} from "../../../SharedModule/individual/TrestleIndividual/trestle-fact";
import {IndividualValueDialog} from "../individual-value.dialog";

@Component({
    selector: "visualize-details",
    templateUrl: "./visualize-details.component.html",
    styleUrls: ["./visualize-details.component.css"]
})
export class VisualizeDetailsComponent implements OnInit, OnDestroy {

    public individual: TrestleIndividual;
    public mapIndividual: Subject<MapSource>;
    public individualFactHistory: IIndividualHistory;
    public minTime: moment.Moment;
    public maxTime: moment.Moment;
    private routeSubscription: Subscription;
    private dialogRef: MatDialogRef<IndividualValueDialog> | null;

    constructor(private is: IndividualService,
                private dialog: MatDialog,
                private viewContainerRef: ViewContainerRef,
                private route: ActivatedRoute) {
        this.mapIndividual = new Subject();
        this.minTime = moment().year(2011).startOf("year");
        this.maxTime = moment().year(2016).endOf("year");
    }

    public ngOnInit(): void {
        this.routeSubscription = this.route.params.subscribe((params) => {
            console.debug("has params", params);
            this.loadIndividual(params["id"]);
        });
    }

    public ngOnDestroy(): void {
        this.routeSubscription.unsubscribe();
    }

    public displayFn(individualName: string): string {
        const strings = individualName.split("#");
        return strings[1];
    }

    public openValueModal(fact: TrestleFact): void {
        const config = new MatDialogConfig();
        config.viewContainerRef = this.viewContainerRef;
        this.dialogRef = this.dialog.open(IndividualValueDialog, config);
        this.dialogRef.componentInstance.name = fact.getName();
        this.dialogRef.componentInstance.value = fact.getValue();
        this.dialogRef.afterClosed().subscribe(() => this.dialogRef = null);
    }

    private loadIndividual(value: string): void {
        console.debug("Loading individual:", value);
        this.is.getTrestleIndividual(value)
            .subscribe((results: TrestleIndividual) => {
                console.debug("has selection", results);
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
                this.mapIndividual.next({
                    id: results.getID(),
                    data: {
                        type: "Feature",
                        geometry: results.getSpatialValue(),
                        id: results.getIDAsInteger().toString(),
                        properties: results.getFactValues()
                    }
                });
            });
    }
}

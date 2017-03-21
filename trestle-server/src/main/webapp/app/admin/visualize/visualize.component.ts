/**
 * Created by nrobison on 3/7/17.
 */
import {Component, OnInit, ViewContainerRef, ViewEncapsulation} from "@angular/core";
import {VisualizeService, ITrestleIndividual, ITrestleFact} from "./visualize.service";
import {FormControl} from "@angular/forms";
import {Observable} from "rxjs";
import {MdDialog, MdDialogConfig, MdDialogRef} from "@angular/material";
import {IndividualValueDialog} from "./individual-value.dialog";

@Component({
    selector: "visualize",
    templateUrl: "./visualize.component.html",
    styleUrls: ["./visualize.component.css"],
    encapsulation: ViewEncapsulation.None
})

export class VisualizeComponent implements OnInit {
    individualName = new FormControl();
    options: Observable<Array<string>>;
    individual: ITrestleIndividual;
    private dialogRef: MdDialogRef<IndividualValueDialog>;

    constructor(private vs: VisualizeService, private dialog: MdDialog, private viewContainerRef: ViewContainerRef) {
    }

    ngOnInit(): void {
        this.options = this.individualName
            .valueChanges
            .debounceTime(400)
            .distinctUntilChanged()
            .switchMap(name => this.vs.searchForIndividual(name));
    }

    onSubmit() {
        console.debug("Submitted", this.individualName.value);
        this.vs.getIndividualAttributes(this.individualName.value)
            .subscribe((results: ITrestleIndividual) => {
                console.debug("has individual", results);
                this.individual = results;
            });
    }

    openValueModal(fact: ITrestleFact): void {
        let config = new MdDialogConfig();
        config.viewContainerRef = this.viewContainerRef;
        this.dialogRef = this.dialog.open(IndividualValueDialog, config);
        this.dialogRef.componentInstance.name = fact.name;
        this.dialogRef.componentInstance.value = fact.value;
        this.dialogRef.afterClosed().subscribe(() => this.dialogRef = null);
    }

    displayFn(individualName: string): string {
        let strings = individualName.split("#");
        return strings[1];
    }
}
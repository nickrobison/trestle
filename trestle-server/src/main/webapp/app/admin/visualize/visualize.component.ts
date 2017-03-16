/**
 * Created by nrobison on 3/7/17.
 */
import {Component, OnInit} from "@angular/core";
import {VisualizeService, ITrestleIndividual} from "./visualize.service";
import {FormControl} from "@angular/forms";
import {Observable} from "rxjs";
import {IIndividualConfig} from "./individual-graph.component";

@Component({
    selector: "visualize",
    templateUrl: "./visualize.component.html",
    styleUrls: ["./visualize.component.css"]
})

export class VisualizeComponent implements OnInit {
    individualName = new FormControl();
    options: Observable<Array<string>>;
    individual: ITrestleIndividual;

    constructor(private vs: VisualizeService) {
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
}
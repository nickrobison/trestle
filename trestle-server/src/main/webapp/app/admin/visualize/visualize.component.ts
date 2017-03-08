/**
 * Created by nrobison on 3/7/17.
 */
import {Component, OnInit} from "@angular/core";
import {VisualizeService} from "./visualize.service";
import {FormControl} from "@angular/forms";
import {Observable} from "rxjs";

@Component({
    selector: "visualize",
    templateUrl: "./visualize.component.html",
    styleUrls: ["./visualize.component.css"]
})

export class VisualizeComponent implements OnInit {
    individualName = new FormControl();
    options: Observable<Array<string>>;

    constructor(private vs: VisualizeService) {}

    ngOnInit(): void {
        this.options = this.individualName
            .valueChanges
            .debounceTime(400)
            .distinctUntilChanged()
            .switchMap(name => this.vs.searchForIndividual(name));
    }

    onSubmit() {
        console.debug("Submitted", this.individualName.value);
    }
}
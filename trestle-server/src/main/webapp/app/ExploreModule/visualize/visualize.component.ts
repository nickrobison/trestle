/**
 * Created by nrobison on 3/7/17.
 */
import {Component, ViewEncapsulation} from "@angular/core";
import {TrestleIndividual} from "../../SharedModule/individual/TrestleIndividual/trestle-individual";
import {Router} from "@angular/router";

@Component({
    selector: "visualize",
    templateUrl: "./visualize.component.html",
    styleUrls: ["./visualize.component.css"],
    encapsulation: ViewEncapsulation.None
})

export class VisualizeComponent {

    constructor(private router: Router) { }

    public selectedOption(value: string) {
        const filteredValue = TrestleIndividual.withoutHostname(value);
        console.debug("Clicked", filteredValue);
        this.router.navigate(["/explore/visualize", filteredValue]);
    }
}

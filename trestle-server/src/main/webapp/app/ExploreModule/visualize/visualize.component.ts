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
        const split = value.split("#");
        console.debug("Split value:", split);
        if (split.length > 1) {
            this.router.navigate(["/explore/visualize", split[1]], {queryParams:
                {root: split[0]}});
        } else {
            this.router.navigate(["/explore/visualize", split[0]]);
        }

    }
}

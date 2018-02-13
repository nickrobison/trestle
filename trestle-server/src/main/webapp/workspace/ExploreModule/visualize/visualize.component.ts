/**
 * Created by nrobison on 3/7/17.
 */
import { Component, ViewEncapsulation } from "@angular/core";
import { Router } from "@angular/router";

@Component({
    selector: "visualize",
    templateUrl: "./visualize.component.html",
    styleUrls: ["./visualize.component.css"],
    encapsulation: ViewEncapsulation.None
})

export class VisualizeComponent {

    constructor(private router: Router) {
    }

    public selectedOption(value: string) {
        // Using a static method from TrestleIndividual will cause Angular to explode, so don't do it, even though it seems to make perfect sense
        this.router.navigate(["/explore/visualize", value]);
    }
}

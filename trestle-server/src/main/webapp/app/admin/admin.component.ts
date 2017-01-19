/**
 * Created by nrobison on 1/18/17.
 */
import {Component, ViewEncapsulation, OnInit} from "@angular/core";

@Component({
    selector: "admin-root",
    templateUrl: "./admin.component.html",
    styleUrls: ["./admin.component.css"],
    // styleUrls: ["../../theme.scss", "./admin.component.css"],
    // encapsulation: ViewEncapsulation.None
})

export class AdminComponent implements OnInit {

    public sideNavOpen: boolean;
    constructor() {}

    ngOnInit(): void {
        this.sideNavOpen = true;
    }
}
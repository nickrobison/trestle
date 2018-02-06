import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";

@Component({
    selector: "introduction",
    templateUrl: "./introduction.component.html",
    styleUrls: ["./introduction.component.css"]
})
export class IntroductionComponent implements OnInit {

    public introductionState: "intro" | "context" | "zoom" | "select";

    public constructor(private router: Router) {
    }

    public ngOnInit(): void {
        this.introductionState = "intro";
    }

    public next(): void {
        if (this.introductionState === "select") {
            this.router.navigate(["/demographics"]);
        } else if (this.introductionState === "intro") {
            this.introductionState = "context";
        } else if (this.introductionState === "context") {
            this.introductionState = "zoom";
        } else if (this.introductionState === "zoom") {
            this.introductionState = "select";
        }
    }
}

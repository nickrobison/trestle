import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";

@Component({
    selector: "introduction",
    templateUrl: "./introduction.component.html",
    styleUrls: ["./introduction.component.css"]
})
export class IntroductionComponent implements OnInit {

    private gotoDemographics: boolean;
    public introductionState: "intro" | "context" | "instructions";

    public constructor(private router: Router) {
        this.gotoDemographics = false;
    }

    public ngOnInit(): void {
        this.introductionState = "intro";
    }


    public next(): void {
        if (this.introductionState === "instructions") {
            this.router.navigate(["/demographics"]);
        } else if (this.introductionState === "intro") {
            this.introductionState = "context";
        } else if (this.introductionState === "context") {
            this.introductionState = "instructions";
        }
    }
}

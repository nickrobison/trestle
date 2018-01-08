import { Component, OnInit } from "@angular/core";
import { Form, FormBuilder, FormControl, FormGroup, Validators } from "@angular/forms";
import { Router } from "@angular/router";

@Component({
    selector: "demographic",
    templateUrl: "./demographics.component.html",
    styleUrls: ["./demographics.component.css"]
})
export class DemographicsComponent implements OnInit {

    public demographics: FormGroup;
    public age: FormControl;
    public education: FormControl;
    public publicHealth: FormControl;
    public geospatial: FormControl;

    public constructor(private fb: FormBuilder, private router: Router) {
    }

    public ngOnInit(): void {
        // this.es.createUser();
        this.age = new FormControl("", Validators.required);
        this.education = new FormControl("", Validators.required);
        this.publicHealth = new FormControl("", Validators.required);
        this.geospatial = new FormControl("", Validators.required);
        this.demographics = this.fb.group({
            age: this.age,
            education: this.education,
            publicHealth: this.publicHealth,
            geospatial: this.geospatial
        });
    }

    public onSubmit(form: Form): void {
        console.debug("Submitted", form);
        //    Submit demographic data to the service
        this.router.navigate(["/experiment"]);
    }
}

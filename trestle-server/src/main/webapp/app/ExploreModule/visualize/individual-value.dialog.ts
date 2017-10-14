/**
 * Created by nrobison on 3/16/17.
 */
import {Component} from "@angular/core";
import { MatDialogRef } from "@angular/material";

@Component({
    selector: "individual-value-dialog",
    template: "<h3 md-dialog-title>{{name}}</h3>" +
    "<div md-dialog-content>{{value}}</div> "
})
export class IndividualValueDialog {

    public name: string;
    public value: string;

    constructor(private dialogRef: MatDialogRef<IndividualValueDialog>) {}
}

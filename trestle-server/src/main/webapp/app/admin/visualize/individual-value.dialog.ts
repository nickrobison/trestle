/**
 * Created by nrobison on 3/16/17.
 */
import {Component, OnInit} from "@angular/core";
import {MdDialogRef} from "@angular/material";

@Component({
    selector: "individual-value-dialog",
    template: "<h3 md-dialog-title>{{name}}</h3>" +
    "<div md-dialog-content>{{value}}</div> "
})
export class IndividualValueDialog {

    name: string;
    value: string;

    constructor(private dialogRef: MdDialogRef<IndividualValueDialog>) {}
}

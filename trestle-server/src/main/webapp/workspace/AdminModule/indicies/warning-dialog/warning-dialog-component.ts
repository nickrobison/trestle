import { Component, Inject } from "@angular/core";
import { MAT_DIALOG_DATA } from "@angular/material";

@Component({
    templateUrl: "./warning-dialog-component.html",
    styleUrls: ["./warning-dialog-component.css"]
})
export class WarningDialogComponent {

    public closeValue = "Ok";
    public object: string;
    public action: string;

    public constructor(@Inject(MAT_DIALOG_DATA) public data: any) {
        this.object = data.object;
        this.action = data.action;
    }
}

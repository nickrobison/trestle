/**
 * Created by nrobison on 2/26/17.
 */
import {Component, AfterViewInit, ViewChild, ElementRef} from "@angular/core";
import * as CodeMirror from "codemirror";

@Component({
    selector: "codemirror",
    templateUrl: "./codemirror.component.html",
    styleUrls: ["./codemirror.component.css"]
})

export class CodeMirrorComponent implements AfterViewInit {
    private _value = "function myScript(){return 100;}\n";

    @ViewChild("code") host: ElementRef;
    private instance: CodeMirror.Editor;

    constructor() {
    }

    ngAfterViewInit(): void {
        this.instance = CodeMirror.fromTextArea(this.host.nativeElement, {
            lineNumbers: true,
            theme: "material",
            mode: {
                name: "javascript",
                globalVars: true
            }
        });
        this.instance.setValue(this._value);
        // this.instance.setSize(400, 300);
    }
}

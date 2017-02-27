/**
 * Created by nrobison on 2/26/17.
 */
import {
    Component, AfterViewInit, ViewChild, ElementRef, Output, EventEmitter, Input, OnChanges,
    SimpleChanges, OnInit
} from "@angular/core";
import * as CodeMirror from "codemirror";

@Component({
    selector: "codemirror",
    templateUrl: "./codemirror.component.html",
    styleUrls: ["./codemirror.component.css"]
})

export class CodeMirrorComponent implements AfterViewInit, OnChanges {

    private _value = "function myScript(){return 100;}\n";

    @ViewChild("code") host: ElementRef;
    @Input() prefixes: string;
    @Output() query = new EventEmitter<string>();
    private instance: CodeMirror.Editor;

    constructor() {
    }

    ngAfterViewInit(): void {
        this.instance = CodeMirror.fromTextArea(this.host.nativeElement, {
            lineNumbers: true,
            theme: "material",
            mode: "sparql"
            // mode: {
            //     name: "javascript",
            //     globalVars: true
            // }
        });
        // console.debug("Setting value", this.prefixes);
        // this.instance.setValue(this.prefixes);
    }

    ngOnChanges(changes: SimpleChanges): void {
        console.debug("Updating text from:", changes["prefixes"].previousValue, "to:", changes["prefixes"].currentValue, "old:", this.prefixes);
        // if ((changes["prefixes"].currentValue !== this.prefixes) && (this.instance != null)) {
        //     console.debug("Updating text", changes["prefixes"]);
            this.prefixes = changes["prefixes"].currentValue;
            if (this.instance != null) {
                console.debug("defined, updating");
                this.instance.setValue(this.prefixes);
            }
            // this.instance.setValue(changes["prefixes"].currentValue);
        // }
    }

    submitQuery() {
        this.query.next(this.instance.getValue());
    }


}

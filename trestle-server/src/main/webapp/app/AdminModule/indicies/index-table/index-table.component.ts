import { Component, Input, OnChanges, SimpleChanges } from "@angular/core";
import { IIndexLeafStatistics } from "../index.service";

@Component({
    selector: "index-table",
    templateUrl: "./index-table.component.html",
    styleUrls: ["./index-table.component.css"]
})
export class IndexTableComponent implements OnChanges {

    @Input()
    public data: IIndexLeafStatistics[];

    public constructor() {

    }

    public ngOnChanges(changes: SimpleChanges): void {
        const data = changes["data"];
        if (data.currentValue !== data.previousValue) {
            console.debug("Tabling new data");
        }
    }


}
import { Component, Input, OnChanges, SimpleChanges } from "@angular/core";
import { IIndexLeafStatistics } from "../index.service";

@Component({
    selector: "index-table",
    templateUrl: "./index-table.component.html",
    styleUrls: ["./index-table.component.scss"]
})
export class IndexTableComponent implements OnChanges {

    @Input()
    public data: IIndexLeafStatistics[];
    public sortedData: IIndexLeafStatistics[];

    public constructor() { }

    public ngOnChanges(changes: SimpleChanges): void {
        const data = changes["data"];
        if (data.currentValue !== data.previousValue) {
            console.debug("Tabling new data");
            this.sortedData = (data.currentValue as IIndexLeafStatistics[])
                .sort((a, b) => a.leafID - b.leafID);
        }
    }

    public printLeaf(leaf: IIndexLeafStatistics): void {
        console.debug(leaf);
    }
}

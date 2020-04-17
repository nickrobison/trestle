/**
 * Created by nrobison on 2/28/17.
 */
import {Component, Input} from "@angular/core";
import {ITrestleResultSet} from "../query.service";

@Component({
    selector: "query-viewer",
    templateUrl: "./query-viewer.component.html",
    styleUrls: ["./query-viewer.component.scss"]
})

export class QueryViewerComponent {
    @Input("data") queryData: ITrestleResultSet;

    constructor() {}

    // ngOnChanges(changes: SimpleChanges): void {
    //     if (!changes["queryData"].isFirstChange()) {
    //         console.debug("Data changed to:", changes["queryData"].currentValue)
    //     }
    // }
}

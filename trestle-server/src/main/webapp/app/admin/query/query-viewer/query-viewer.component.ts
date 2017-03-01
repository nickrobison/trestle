/**
 * Created by nrobison on 2/28/17.
 */
import {Component, OnChanges, SimpleChanges, Input, Pipe, PipeTransform} from "@angular/core";
import {ITrestleResultSet} from "../query.service";

@Component({
    selector: "query-viewer",
    templateUrl: "./query-viewer.component.html",
    styleUrls: ["./query-viewer.component.css"]
})

export class QueryViewer {
    @Input("data") queryData: ITrestleResultSet;

    constructor() {}

    // ngOnChanges(changes: SimpleChanges): void {
    //     if (!changes["queryData"].isFirstChange()) {
    //         console.debug("Data changed to:", changes["queryData"].currentValue)
    //     }
    // }
}

@Pipe({name: "mapKeys"})
export class MapKeysPipe implements PipeTransform {
    transform(value: any, ...args: any[]): any {
        let returnArray: Array<any> = [];
        Object.keys(value).forEach(key => {
            returnArray.push({
                key: key,
                value: value[key],
            })
        });
        // value.forEach((entryVal: any, entryKey: any) => {
        //     returnArray.push({
        //         key: entryKey,
        //         value: entryVal
        //     });
        // });
        return returnArray;
    }
}
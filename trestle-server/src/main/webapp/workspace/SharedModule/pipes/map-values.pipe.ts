/**
 * Created by nrobison on 7/11/17.
 */
import { Pipe, PipeTransform } from "@angular/core";

@Pipe({name: "mapValues"})
export class MapValuesPipe implements PipeTransform {
    public transform(value: Map<any, any>, ...args: any[]): any {
        console.debug("Piping:", value);
        const returnArray: any[] = [];
        value.forEach((entryVal: any, entryKey: any) => {
            console.debug("Pushing:", entryKey, entryVal);
            returnArray.push({
                key: entryKey,
                value: entryVal
            });
        });
        return returnArray;
    }
}

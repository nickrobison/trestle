/**
 * Created by nrobison on 2/24/17.
 */
import {
    Component, ViewEncapsulation, OnInit
} from "@angular/core";
import {QueryService, ITrestleResultSet} from "./query.service";

@Component({
    selector: "query-engine",
    templateUrl: "./query.component.html",
    styleUrls: ["./query.component.css"],
    encapsulation: ViewEncapsulation.None
})

export class QueryComponent implements OnInit {

    prefixes: string = "";
    results: ITrestleResultSet = null;

    constructor(private queryService: QueryService) { }

    executeQuery(queryString: string) {
        console.debug("Executing query:", queryString);
        this.queryService.executeQuery(queryString)
            .subscribe((result: ITrestleResultSet) => {
            console.debug("Results:", result);
            this.results = result;
            }, (error) => {
            console.error(error);
            });
    }

    ngOnInit(): void {
        this.queryService.getPrefixes()
            .subscribe(prefixObject => {
                console.debug("has prefixObject:", prefixObject);
                let prefixString: Array<string> = [];
                Object.keys(prefixObject).forEach(key => {
                    console.debug("Key:", key, "Value:", prefixObject[key]);
                    prefixString.push("PREFIX ", key, " <", prefixObject[key], ">\n");
                });
                console.debug("Built string:", prefixString.join(""));
                this.prefixes = prefixString.join("");
                // prefixObject.forEach((key, value) => {
                //     console.debug("Key:", key, "Value:", value);
                // });
                // prefixObject.fo()
                //     .next(entry => {
                //         console.debug(entry);
                //     })
            });
    }
}
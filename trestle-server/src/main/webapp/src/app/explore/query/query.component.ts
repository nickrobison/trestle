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
    styleUrls: ["./query.component.scss"],
    encapsulation: ViewEncapsulation.None
})

export class QueryComponent implements OnInit {

    public prefixes: string = "";
    public results: ITrestleResultSet | null = null;
    public errorMessage: string | null = null;
    public loading = false;

    constructor(private queryService: QueryService) { }

    public ngOnInit(): void {
        this.queryService.getPrefixes()
            .subscribe((prefixObject) => {
                console.debug("has prefixObject:", prefixObject);
                const prefixString: string[] = [];
                Object.keys(prefixObject).forEach(key => {
                    console.debug("Key:", key, "Value:", prefixObject[key]);
                    if (key === ":") {
                        prefixString.push("BASE ", "<", prefixObject[key], ">\n");
                    }
                    prefixString.push("PREFIX ", key, " <", prefixObject[key], ">\n");
                });
                console.debug("Built string:", prefixString.join(""));
                this.prefixes = prefixString.join("");
            });
    }

    /**
     * Execute SPARQL query against the databse
     * @param {string} queryString
     */
    public executeQuery(queryString: string) {
        console.debug("Executing query:", queryString);
        this.loading = true;
        this.errorMessage = null;
        this.queryService.executeQuery(queryString)
            .subscribe((result: ITrestleResultSet) => {
                console.debug("Results:", result);
                this.loading = false;
                this.results = result;
            }, (error: Response) => {
                console.error(error);
                this.loading = false;
                this.results = null;
                this.errorMessage = error.toString();
            });
    }
}

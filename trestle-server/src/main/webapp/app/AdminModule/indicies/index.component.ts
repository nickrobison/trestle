import { AfterViewInit, Component } from "@angular/core";
import { IIndexLeafStatistics, IndexService } from "./index.service";

@Component({
    selector: "index-root",
    templateUrl: "./index.component.html",
    styleUrls: ["./index.component.css"]
})
export class IndexComponent implements AfterViewInit {

    public validStats: IIndexLeafStatistics[];
    public dbStats: IIndexLeafStatistics[];

    public constructor(private is: IndexService) {

    }

    public ngAfterViewInit(): void {
        this.is.getIndexStatistics()
            .subscribe((data) => {
                console.debug("Data:", data);
                this.validStats = data.validLeafStats;
                this.dbStats = data.dbLeafStats;
            });
    }
}

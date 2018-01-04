import { AfterViewInit, Component } from "@angular/core";
import { ICacheStatistics, IIndexLeafStatistics, IndexService } from "./index.service";
import { MatDialog } from "@angular/material";
import { WarningDialogComponent } from "./warning-dialog/warning-dialog-component";

@Component({
    selector: "index-root",
    templateUrl: "./index.component.html",
    styleUrls: ["./index.component.css"]
})
export class IndexComponent implements AfterViewInit {

    public stats: ICacheStatistics;
    public validStats: IIndexLeafStatistics[];
    public dbStats: IIndexLeafStatistics[];
    public validHover: string;
    public dbHover: string;

    public constructor(private is: IndexService, private dialog: MatDialog) {
        this.validHover = "";
        this.dbHover = "";
    }

    public ngAfterViewInit(): void {
        this.is.getIndexStatistics()
            .subscribe((data) => {
                console.debug("Data:", data);
                this.stats = data;
                this.validStats = data.validLeafStats;
                this.dbStats = data.dbLeafStats;
            });
    }

    public updateSelected(event: string, updateVariable: string): void {
        updateVariable = event;
    }

    public openDialog(action: "Rebuild" | "Purge", object: string): void {
        const dialogRef = this.dialog.open(WarningDialogComponent, {
            data: {
                object,
                action
            }
        });
        dialogRef.afterClosed().subscribe((closed) => {
            if (closed) {
                console.debug("Closed", closed);
                // Rebuild the specified index
                if (action === "Rebuild") {
                    // Get the first word as the index type
                    const indexName = object.split(" ")[0];
                    this.is.rebuildIndex(indexName)
                        .subscribe(() => {
                            console.debug("%s is rebuilt", indexName);
                        });
                // Purge the cache, which drops the index
                }
            }
        });

    }
}

import { AfterViewInit, Component } from "@angular/core";
import { ICacheStatistics, IIndexLeafStatistics, IndexService } from "./index.service";
import { WarningDialogComponent } from "./warning-dialog/warning-dialog-component";
import {MatDialog} from '@angular/material/dialog';

@Component({
    selector: "index-root",
    templateUrl: "./index.component.html",
    styleUrls: ["./index.component.scss"]
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
        this.loadStatistics();
    }

    /**
     * Update the currently selected triangle, in case we want to do something with it
     * @param {string} event
     * @param {string} updateVariable
     */
    public updateSelected(event: string, updateVariable: string): void {
        updateVariable = event;
    }

    /**
     * Open modal to perform index maintenance
     * @param {"Rebuild" | "Purge"} action to perform
     * @param {string} object which cache are we using
     */
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
                            console.debug("%s Index is rebuilt", indexName);
                            //    Finally, reload the data on the page
                            this.loadStatistics();
                        });
                // Purge the cache, which drops the index
                } else {
                    const cacheName = object.split(" ")[0];
                    this.is.purgeCache(cacheName)
                        .subscribe(() => {
                            console.debug("%s Cache has been purged", cacheName);
                            //    Finally, reload the data on the page
                            this.loadStatistics();
                        });
                }
            }
        });

    }

    private loadStatistics(): void {
        this.is.getIndexStatistics()
            .subscribe((data) => {
                console.debug("Data:", data);
                this.stats = data;
                this.validStats = data.validLeafStats;
                this.dbStats = data.dbLeafStats;
            });
    }
}

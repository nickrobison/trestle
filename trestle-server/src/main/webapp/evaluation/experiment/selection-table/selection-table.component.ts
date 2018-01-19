import { AfterViewInit, Component, Input, OnChanges, SimpleChanges } from "@angular/core";
import { BehaviorSubject } from "rxjs/BehaviorSubject";
import { MatTableDataSource } from "@angular/material";
import { SelectionModel } from "@angular/cdk/collections";

@Component({
    selector: "selection-table",
    templateUrl: "./selection-table.component.html",
    styleUrls: ["./selection-table.component.css"]
})
export class SelectionTableComponent implements OnChanges, AfterViewInit {

    @Input()
    public data: string[];
    public displayedColumns: string[];
    public tableData: MatTableDataSource<string>;
    public selection: SelectionModel<string>;

    private dataSubject: BehaviorSubject<undefined | string[]>;

    public constructor() {
        this.displayedColumns = ["select", "number"];
        this.selection = new SelectionModel<string>(true, []);
        this.tableData = new MatTableDataSource<string>();
        this.dataSubject = new BehaviorSubject(undefined);
    }

    public ngAfterViewInit(): void {
        this.dataSubject.subscribe((data) => {
            if (data) {
                console.debug("Has new data from subject");
                this.tableData = new MatTableDataSource<string>(data);
            }
        });
    }

    public ngOnChanges(changes: SimpleChanges): void {
        const dataChanges = changes["data"];
        if (!dataChanges.firstChange && dataChanges.firstChange !== dataChanges.previousValue) {
            this.dataSubject.next(dataChanges.currentValue);
        }
    }

    public isAllSelected(): boolean {
        const numSelected = this.selection.selected.length;
        const numRows = this.tableData.data.length;
        return numSelected === numRows;
    }

    public masterToggle(): void {
        this.isAllSelected() ?
            this.selection.clear() :
            this.tableData.data.forEach((row) => this.selection.select(row));
    }

    public getSelectedRows(): string[] {
        return this.selection.selected;
    }

    // These functions are needed because the SelectionModel throws an error if called inside a template, because, why not?

    public hasValue(): boolean {
        return this.selection.hasValue();
    }

    public rowSelected(row: string): boolean {
        return this.selection.isSelected(row);
    }

    public toggleRow(row: string): void {
        this.selection.toggle(row);
    }
}

import { AfterViewInit, Component, EventEmitter, Input, OnChanges, Output, Pipe, PipeTransform, SimpleChanges } from "@angular/core";
import { BehaviorSubject } from "rxjs/BehaviorSubject";
import { MatTableDataSource } from "@angular/material";
import { SelectionModel } from "@angular/cdk/collections";

export interface ITableData {
    idxValue: number;
    color: string;
}

@Component({
    selector: "selection-table",
    templateUrl: "./selection-table.component.html",
    styleUrls: ["./selection-table.component.css"]
})
export class SelectionTableComponent implements OnChanges, AfterViewInit {

    @Input()
    public data: ITableData[];
    @Output()
    public minimalSelection: EventEmitter<boolean>;
    public displayedColumns: string[];
    public tableData: MatTableDataSource<ITableData>;
    public selection: SelectionModel<ITableData>;

    private dataSubject: BehaviorSubject<undefined | ITableData[]>;
    private minimalSelectionValue: boolean;

    public constructor() {
        this.displayedColumns = ["select", "number", "color"];
        this.selection = new SelectionModel<ITableData>(true, []);
        this.tableData = new MatTableDataSource<ITableData>();
        this.dataSubject = new BehaviorSubject(undefined);

        this.minimalSelection = new EventEmitter<boolean>();
        this.minimalSelectionValue = false;
    }

    public ngAfterViewInit(): void {
        this.dataSubject.subscribe((data) => {
            if (data) {
                console.debug("Has new data from subject");
                // Strip out the names and only return the index in the list
                this.tableData = new MatTableDataSource<ITableData>(data);
            }
        });
    }

    public ngOnChanges(changes: SimpleChanges): void {
        const dataChanges = changes["data"];
        if (!dataChanges.firstChange && dataChanges.firstChange !== dataChanges.previousValue) {
            this.dataSubject.next(dataChanges.currentValue);
        }
    }

    public reset(): void {
        this.data = [];
        this.selection.clear();
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

    public getSelectedRows(): ITableData[] {
        return this.selection.selected;
    }

    // These functions are needed because the SelectionModel throws an error if called inside a template, because, why not?

    public hasValue(): boolean {
        return this.selection.hasValue();
    }

    public rowSelected(row: ITableData): boolean {
        return this.selection.isSelected(row);
    }

    public toggleRow(row: ITableData): void {
        if (!this.rowSelected(row) && !this.minimalSelectionValue) {
            this.minimalSelection.next(true);
            this.minimalSelectionValue = true;
        }
        this.selection.toggle(row);
    }
}

@Pipe({name: "counter"})
export class CounterPipe implements PipeTransform {
    public transform(value: string): number {
        return Number.parseInt(value) + 1;
    }

}


import {Component, Input, ViewContainerRef} from '@angular/core';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {TrestleFact} from "../../../../shared/individual/TrestleIndividual/trestle-fact";
import {MatDialog, MatDialogConfig, MatDialogRef} from "@angular/material/dialog";
import {IndividualValueDialog} from "../../individual-value.dialog";

@Component({
  selector: 'visualize-fact-table',
  templateUrl: './fact-table.component.html',
  styleUrls: ['./fact-table.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({height: '0px', minHeight: '0'})),
      state('expanded', style({height: '*'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ],
})
export class FactTableComponent {

  @Input()
  facts: TrestleFact[];
  columnsToDisplay = ["name", "type", "value", "from", "to"];
  expandedElement: TrestleFact | null;
  private dialogRef: MatDialogRef<IndividualValueDialog> | null;

  constructor(
    private dialog: MatDialog,
    private viewContainerRef: ViewContainerRef) { }

  /**
   * Open the value Modal and display the given fact value
   * @param {TrestleFact} fact
   */
  public openValueModal(fact: TrestleFact): void {
    const config = new MatDialogConfig();
    config.viewContainerRef = this.viewContainerRef;
    this.dialogRef = this.dialog.open(IndividualValueDialog, config);
    this.dialogRef.componentInstance.name = fact.getName();
    this.dialogRef.componentInstance.value = fact.getValue();
    this.dialogRef.afterClosed().subscribe(() => this.dialogRef = null);
  }

}

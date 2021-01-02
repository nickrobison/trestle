import {Component, Input, OnChanges, OnInit, SimpleChanges, ViewContainerRef} from '@angular/core';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {TrestleFact} from "../../../../shared/individual/TrestleIndividual/trestle-fact";
import {MatDialog, MatDialogConfig, MatDialogRef} from "@angular/material/dialog";
import {IndividualValueDialog} from "../../individual-value.dialog";
import {filter, flow, groupBy, mapValues, orderBy} from 'lodash/fp';

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
export class FactTableComponent implements OnInit, OnChanges {

  @Input()
  facts: TrestleFact[];
  columnsToDisplay = ["name", "type", "value", "from", "to"];
  expandedElement: TrestleFact | null;
  data: TrestleFact[][] = [];
  private dialogRef: MatDialogRef<IndividualValueDialog> | null;


  constructor(
    private dialog: MatDialog,
    private viewContainerRef: ViewContainerRef) {
    this.groupData();
  }

  ngOnInit(): void {
    this.groupData();
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.facts = changes["facts"].currentValue;
    this.groupData();
    // This is where we should call refresh on the table.
  }


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

  private groupData() {
    const d = flow(
      // Eventually we'll want to remove this filter, but it's good enough for now.
      filter((f: TrestleFact) => f.getDatabaseTemporal().isContinuing()),
      groupBy((x: TrestleFact) => x.getName()),
      mapValues((v: TrestleFact[]) => orderBy(v => v.getDatabaseTemporal().getFrom(), "desc", v)),
      mapValues((v: TrestleFact[]) => orderBy(v => v.getValidTemporal().getFrom(), "desc", v)),
    )(this.facts);
    this.data = Object.values(d);
  }
}

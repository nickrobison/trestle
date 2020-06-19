/**
 * Created by nrobison on 2/28/17.
 */
import {Component, Input, OnChanges, OnInit, SimpleChanges, ViewChild} from '@angular/core';
import {ITrestleResultSet} from '../query.service';
import {MatTableDataSource} from '@angular/material/table';
import {MatPaginator} from '@angular/material/paginator';

@Component({
  selector: 'query-viewer',
  templateUrl: './query-viewer.component.html',
  styleUrls: ['./query-viewer.component.scss']
})

export class QueryViewerComponent implements OnInit, OnChanges {

  @Input('data')
  public queryData: ITrestleResultSet = {
    rows: 0,
    bindingNames: [],
    results: []
  };
  @ViewChild(MatPaginator)
  public paginator: MatPaginator;
  public dataSource = new MatTableDataSource<ITrestleResultSet>([]);

  constructor() {
    // Not used
  }

  ngOnInit(): void {
    this.dataSource.paginator = this.paginator;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['queryData'].isFirstChange()) {
      console.debug('Data changed to:', changes['queryData'].currentValue);
      this.dataSource.data = changes['queryData'].currentValue.results;
    }
  }
}

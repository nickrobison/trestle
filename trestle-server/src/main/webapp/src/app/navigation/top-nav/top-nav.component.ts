import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {Observable} from 'rxjs';
import {TrestleUser} from '../../user/trestle-user';
import {select, Store} from '@ngrx/store';
import {selectUserFromUser, State} from '../../reducers';

@Component({
  selector: 'top-nav',
  templateUrl: './top-nav.component.html',
  styleUrls: ['./top-nav.component.scss']
})
export class TopNavComponent implements OnInit {
  public user: Observable<TrestleUser>;

  @Output()
  public headerClicked = new EventEmitter<void>();

  constructor(private store: Store<State>) {
  }

  ngOnInit(): void {
    this.user = this.store.pipe(select(selectUserFromUser));
  }
}

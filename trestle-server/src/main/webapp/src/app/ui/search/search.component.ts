import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {MatAutocompleteSelectedEvent} from '@angular/material/autocomplete';
import {FormControl} from '@angular/forms';
import {catchError, debounceTime, switchMap, tap} from 'rxjs/operators';
import {Observable} from 'rxjs';
import {IndividualService} from '../../shared/individual/individual.service';
import {TrestleIndividual} from '../../shared/individual/TrestleIndividual/trestle-individual';
import {State} from '../../reducers';
import {Store} from '@ngrx/store';
import {addNotification} from '../../actions/notification.actions';
import {TrestleError} from '../../reducers/notification.reducers';

@Component({
  selector: 'search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit {

  @Input()
  public showError = true;
  public options: Observable<string[]>;
  public individualName = new FormControl();
  public errorText = '';
  @Output() public selected = new EventEmitter<string>();

  public constructor(private is: IndividualService, private store: Store<State>) {
  }

  public ngOnInit(): void {
    this.options = this.individualName
      .valueChanges
      .pipe(
        debounceTime(400),
        tap(() => this.errorText = ''),
        switchMap((name) => this.is.searchForIndividual(name)
          .pipe(
            catchError(() => {
              // Create error
              const tError: TrestleError = {
                state: 'error',
                error: new Error('Cannot search for individuals'),
              };
              this.store.dispatch(addNotification({notification: tError}));
              this.errorText = 'Cannot search for individual';
              return [];
            }))
        ));
  }

  /**
   * Function to filter individual ID by extracting the suffix
   * @param {string} name
   * @returns {string}
   */
  public displayFn(name: string): string {
    return TrestleIndividual.extractSuffix(name);
  }

  /**
   * Selection handler
   * @param {MatAutocompleteSelectedEvent} event
   */
  public selectHandler = (event: MatAutocompleteSelectedEvent): void => {
    this.selected.next(event.option.value);
  };
}

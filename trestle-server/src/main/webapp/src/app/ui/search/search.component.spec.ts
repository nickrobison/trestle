import {ComponentFixture, fakeAsync, TestBed, tick, waitForAsync} from '@angular/core/testing';

import {SearchComponent} from './search.component';
import {INDIVIDUAL_CACHE, IndividualService} from '../../shared/individual/individual.service';
import {MaterialModule} from '../../material/material.module';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {HttpClientTestingModule} from '@angular/common/http/testing';
import {CACHE_SERVICE_CONFIG} from '../../shared/cache/cache.service.config';
import {INDIVIDUAL_CACHE_DI_CONFIG} from '../../explore/explore.config';
import {CacheService} from '../../shared/cache/cache.service';
import {TrestleIndividual} from '../../shared/individual/TrestleIndividual/trestle-individual';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {MockStore, provideMockStore} from '@ngrx/store/testing';
import {initialAppState} from '../../reducers';
import {addNotification} from '../../actions/notification.actions';
import {TrestleError} from '../../reducers/notification.reducers';
import {of, throwError} from 'rxjs';

describe('SearchComponent', () => {
  let component: SearchComponent;
  let fixture: ComponentFixture<SearchComponent>;
  let mockStore: MockStore;
  let is: IndividualService;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [SearchComponent],
      providers: [IndividualService, provideMockStore({
        initialState: initialAppState,
      }), {
        provide: CACHE_SERVICE_CONFIG, useValue: INDIVIDUAL_CACHE_DI_CONFIG
      }, {
        provide: INDIVIDUAL_CACHE, useFactory: () => (new CacheService<string, TrestleIndividual>(INDIVIDUAL_CACHE_DI_CONFIG))
      }],
      imports: [MaterialModule, ReactiveFormsModule, FormsModule, HttpClientTestingModule, NoopAnimationsModule]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchComponent);
    component = fixture.componentInstance;
    mockStore = TestBed.inject(MockStore);
    is = TestBed.inject(IndividualService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have an error', fakeAsync(() => {
    const dispatchSpy = spyOn(mockStore, 'dispatch');
    const isSpy = spyOn(is, 'searchForIndividual').and.returnValue(throwError('Cannot search for individuals'));
    component.individualName.setValue('test');
    fixture.detectChanges();
    tick(400); // Advance past the debounce time
    const tError: TrestleError = {
      state: 'error',
      error: new Error('Cannot search for individuals'),
    };
    expect(isSpy).toBeCalledTimes(1);
    expect(dispatchSpy).toBeCalledTimes(1);
    expect(dispatchSpy).toBeCalledWith(addNotification({
      notification: tError,
    }));
  }));

  it('should show values', fakeAsync(() => {
    const dispatchSpy = spyOn(mockStore, 'dispatch');
    const testValues = ['test', 'values', 'go', 'here'];
    const isSpy = spyOn(is, 'searchForIndividual').and.returnValue(of(testValues.map(v => `https://test.local/${v}`)));
    component.individualName.setValue('test');
    fixture.detectChanges();
    tick(400); // Advance past the debounce time
    expect(dispatchSpy).toBeCalledTimes(0);
    component.options.subscribe(results => {
      expect(results).toEqual(testValues);
    });
  }));
});

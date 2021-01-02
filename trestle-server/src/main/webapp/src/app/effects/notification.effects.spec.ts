import {TestBed} from '@angular/core/testing';
import {provideMockActions} from '@ngrx/effects/testing';
import {Observable} from 'rxjs';

import {NotificationEffects} from './notification.effects';

describe('NotificationEffects', () => {
  let actions$: Observable<any>;
  let effects: NotificationEffects;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        NotificationEffects,
        provideMockActions(() => actions$)
      ]
    });

    effects = TestBed.inject(NotificationEffects);
  });

  it('should be created', () => {
    expect(effects).toBeTruthy();
  });
});

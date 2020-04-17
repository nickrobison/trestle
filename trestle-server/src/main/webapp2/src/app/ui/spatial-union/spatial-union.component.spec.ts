import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {SpatialUnionComponent} from './spatial-union.component';
import {INDIVIDUAL_CACHE, IndividualService} from '../../shared/individual/individual.service';
import {EventGraphComponent} from '../event-graph/event-graph.component';
import {HttpClientTestingModule} from '@angular/common/http/testing';
import {CACHE_SERVICE_CONFIG} from '../../shared/cache/cache.service.config';
import {INDIVIDUAL_CACHE_DI_CONFIG} from '../../explore/explore.config';
import {CacheService} from '../../shared/cache/cache.service';
import {TrestleIndividual} from '../../shared/individual/TrestleIndividual/trestle-individual';

describe('SpatialUnionComponent', () => {
  let component: SpatialUnionComponent;
  let fixture: ComponentFixture<SpatialUnionComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [SpatialUnionComponent, EventGraphComponent],
      providers: [IndividualService, {
        provide: CACHE_SERVICE_CONFIG, useValue: INDIVIDUAL_CACHE_DI_CONFIG
      }, {
        provide: INDIVIDUAL_CACHE, useFactory: () => (new CacheService<string, TrestleIndividual>(INDIVIDUAL_CACHE_DI_CONFIG))
      }],
      imports: [HttpClientTestingModule]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SpatialUnionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

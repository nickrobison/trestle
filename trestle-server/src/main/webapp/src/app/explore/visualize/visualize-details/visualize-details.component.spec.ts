import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import {VisualizeDetailsComponent} from './visualize-details.component';
import {MaterialModule} from '../../../material/material.module';
import {SharedModule} from '../../../shared/shared.module';
import {RouterTestingModule} from '@angular/router/testing';
import {INDIVIDUAL_CACHE, IndividualService} from '../../../shared/individual/individual.service';
import {TrestleMapComponent} from '../../../ui/trestle-map/trestle-map.component';
import {HistoryGraphComponent} from '../../../ui/history-graph/history-graph.component';
import {IndividualGraphComponent} from '../individual-graph/individual-graph.component';
import {SpatialUnionComponent} from '../../../ui/spatial-union/spatial-union.component';
import {EventGraphComponent} from '../../../ui/event-graph/event-graph.component';
import {HttpClientTestingModule} from '@angular/common/http/testing';
import {CACHE_SERVICE_CONFIG} from '../../../shared/cache/cache.service.config';
import {INDIVIDUAL_CACHE_DI_CONFIG} from '../../explore.config';
import {CacheService} from '../../../shared/cache/cache.service';
import {TrestleIndividual} from '../../../shared/individual/TrestleIndividual/trestle-individual';

describe('VisualizeDetailsComponent', () => {
  let component: VisualizeDetailsComponent;
  let fixture: ComponentFixture<VisualizeDetailsComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [VisualizeDetailsComponent, TrestleMapComponent, HistoryGraphComponent, IndividualGraphComponent, SpatialUnionComponent, EventGraphComponent],
      providers: [IndividualService, {
        provide: CACHE_SERVICE_CONFIG, useValue: INDIVIDUAL_CACHE_DI_CONFIG
      }, {
        provide: INDIVIDUAL_CACHE, useFactory: () => (new CacheService<string, TrestleIndividual>(INDIVIDUAL_CACHE_DI_CONFIG))
      }],
      imports: [MaterialModule, SharedModule, RouterTestingModule, HttpClientTestingModule]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(VisualizeDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

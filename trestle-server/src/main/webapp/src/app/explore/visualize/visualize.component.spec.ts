import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import {VisualizeComponent} from './visualize.component';
import {RouterTestingModule} from '@angular/router/testing';
import {MaterialModule} from '../../material/material.module';
import {UiModule} from '../../ui/ui.module';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {HttpClientTestingModule} from '@angular/common/http/testing';
import {CACHE_SERVICE_CONFIG} from '../../shared/cache/cache.service.config';
import {INDIVIDUAL_CACHE_DI_CONFIG} from '../explore.config';
import {INDIVIDUAL_CACHE} from '../../shared/individual/individual.service';
import {CacheService} from '../../shared/cache/cache.service';
import {TrestleIndividual} from '../../shared/individual/TrestleIndividual/trestle-individual';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {provideMockStore} from '@ngrx/store/testing';

describe('VisualizeComponent', () => {
  let component: VisualizeComponent;
  let fixture: ComponentFixture<VisualizeComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, MaterialModule, UiModule, ReactiveFormsModule, FormsModule, HttpClientTestingModule, NoopAnimationsModule],
      providers: [provideMockStore(), {
        provide: CACHE_SERVICE_CONFIG, useValue: INDIVIDUAL_CACHE_DI_CONFIG
      }, {
        provide: INDIVIDUAL_CACHE, useFactory: () => (new CacheService<string, TrestleIndividual>(INDIVIDUAL_CACHE_DI_CONFIG))
      }],
      declarations: [VisualizeComponent]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(VisualizeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

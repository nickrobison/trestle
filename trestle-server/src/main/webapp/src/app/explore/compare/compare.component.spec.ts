import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {CompareComponent} from './compare.component';
import {MaterialModule} from '../../material/material.module';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {UiModule} from '../../ui/ui.module';
import {ExporterComponent} from '../exporter/exporter.component';
import {SharedModule} from '../../shared/shared.module';
import {RouterTestingModule} from '@angular/router/testing';
import {HttpClientTestingModule} from '@angular/common/http/testing';
import {CACHE_SERVICE_CONFIG} from '../../shared/cache/cache.service.config';
import {COLOR_DI_CONFIG, INDIVIDUAL_CACHE_DI_CONFIG} from '../explore.config';
import {INDIVIDUAL_CACHE} from '../../shared/individual/individual.service';
import {CacheService} from '../../shared/cache/cache.service';
import {TrestleIndividual} from '../../shared/individual/TrestleIndividual/trestle-individual';
import {MapService} from '../viewer/map.service';
import {COLOR_SERVICE_CONFIG} from '../../shared/color/color-service.config';

describe.skip('CompareComponent', () => {
  let component: CompareComponent;
  let fixture: ComponentFixture<CompareComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [CompareComponent, ExporterComponent],
      providers: [{
        provide: CACHE_SERVICE_CONFIG, useValue: INDIVIDUAL_CACHE_DI_CONFIG
      }, {
        provide: INDIVIDUAL_CACHE, useFactory: () => (new CacheService<string, TrestleIndividual>(INDIVIDUAL_CACHE_DI_CONFIG))
      }, {
        provide: COLOR_SERVICE_CONFIG, useValue: COLOR_DI_CONFIG
      }, MapService],
      imports: [MaterialModule, ReactiveFormsModule, FormsModule, UiModule, SharedModule, RouterTestingModule, HttpClientTestingModule]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CompareComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

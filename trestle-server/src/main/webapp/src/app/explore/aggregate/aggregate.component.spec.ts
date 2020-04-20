import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {AggregateComponent} from './aggregate.component';
import {SharedModule} from '../../shared/shared.module';
import {MaterialModule} from '../../material/material.module';
import {UiModule} from '../../ui/ui.module';
import {AggregationService} from './aggregation.service';
import {ReactiveFormsModule} from '@angular/forms';
import {MapService} from '../viewer/map.service';
import {HttpClientTestingModule} from '@angular/common/http/testing';

describe.skip('AggregateComponent', () => {
  let component: AggregateComponent;
  let fixture: ComponentFixture<AggregateComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [AggregateComponent],
      providers: [AggregationService, MapService],
      imports: [SharedModule, MaterialModule, UiModule, ReactiveFormsModule, HttpClientTestingModule]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AggregateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

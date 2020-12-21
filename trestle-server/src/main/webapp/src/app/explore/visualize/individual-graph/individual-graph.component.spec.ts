import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { IndividualGraphComponent } from './individual-graph.component';
import {MaterialModule} from '../../../material/material.module';

describe('IndividualGraphComponent', () => {
  let component: IndividualGraphComponent;
  let fixture: ComponentFixture<IndividualGraphComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ IndividualGraphComponent ],
      imports: [MaterialModule]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(IndividualGraphComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toMatchSnapshot();
  });
});

import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { IndividualGraphComponent } from './individual-graph.component';
import {MaterialModule} from '../../../material/material.module';

describe('IndividualGraphComponent', () => {
  let component: IndividualGraphComponent;
  let fixture: ComponentFixture<IndividualGraphComponent>;

  beforeEach(async(() => {
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

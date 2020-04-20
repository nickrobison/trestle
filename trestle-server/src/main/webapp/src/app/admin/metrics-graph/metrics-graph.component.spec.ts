import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MetricsGraphComponent } from './metrics-graph.component';

describe('MetricsGraphComponent', () => {
  let component: MetricsGraphComponent;
  let fixture: ComponentFixture<MetricsGraphComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MetricsGraphComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MetricsGraphComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toMatchSnapshot();
  });
});

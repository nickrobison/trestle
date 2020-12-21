import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { MetricsGraphComponent } from './metrics-graph.component';

describe('MetricsGraphComponent', () => {
  let component: MetricsGraphComponent;
  let fixture: ComponentFixture<MetricsGraphComponent>;

  beforeEach(waitForAsync(() => {
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

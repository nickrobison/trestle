import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { HistoryGraphComponent } from './history-graph.component';

describe('HistoryGraphComponent', () => {
  let component: HistoryGraphComponent;
  let fixture: ComponentFixture<HistoryGraphComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ HistoryGraphComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(HistoryGraphComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toMatchSnapshot();
  });
});

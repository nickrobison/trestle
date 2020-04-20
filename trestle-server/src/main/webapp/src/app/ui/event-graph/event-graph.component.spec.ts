import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { EventGraphComponent } from './event-graph.component';

describe('EventGraphComponent', () => {
  let component: EventGraphComponent;
  let fixture: ComponentFixture<EventGraphComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ EventGraphComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EventGraphComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toMatchSnapshot();
  });
});

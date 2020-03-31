import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TrestleMapComponent } from './trestle-map.component';

describe('TrestleMapComponent', () => {
  let component: TrestleMapComponent;
  let fixture: ComponentFixture<TrestleMapComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TrestleMapComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TrestleMapComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

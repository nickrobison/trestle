import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import {QueryViewerComponent} from './query-viewer.component';
import {MaterialModule} from '../../../material/material.module';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';

describe('QueryViewerComponent', () => {
  let component: QueryViewerComponent;
  let fixture: ComponentFixture<QueryViewerComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [MaterialModule, NoopAnimationsModule],
      declarations: [ QueryViewerComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(QueryViewerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

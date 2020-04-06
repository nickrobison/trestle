import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {WarningDialogComponent} from './warning-dialog-component';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';

describe('WarningDialogComponent', () => {
  let component: WarningDialogComponent;
  let fixture: ComponentFixture<WarningDialogComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      // imports: [MaterialModule, ReactiveFormsModule, FormsModule, HttpClientTestingModule],
      providers: [{
        provide: MAT_DIALOG_DATA, useValue: ''
      }],
      declarations: [WarningDialogComponent],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WarningDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toMatchSnapshot();
  });
});

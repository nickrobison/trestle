import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {IndexTableComponent} from './index-table.component';
import {MaterialModule} from '../../../material/material.module';

describe('IndexTableComponent', () => {
  let component: IndexTableComponent;
  let fixture: ComponentFixture<IndexTableComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [IndexTableComponent],
      imports: [MaterialModule],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(IndexTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toMatchSnapshot();
  });
});

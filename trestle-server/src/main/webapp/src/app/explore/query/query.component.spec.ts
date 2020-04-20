import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {QueryComponent} from './query.component';
import {QueryService} from './query.service';
import {HttpClientTestingModule} from '@angular/common/http/testing';
import {MaterialModule} from '../../material/material.module';
import {CodeMirrorComponent} from './codemirror/codemirror.component';
import {QueryViewerComponent} from './query-viewer/query-viewer.component';

describe('QueryComponent', () => {
  let component: QueryComponent;
  let fixture: ComponentFixture<QueryComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, MaterialModule],
      declarations: [QueryComponent, CodeMirrorComponent, QueryViewerComponent],
      providers: [QueryService]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(QueryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

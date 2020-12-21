import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import {CodeMirrorComponent} from './codemirror.component';

describe('CodeMirrorComponent', () => {
  let component: CodeMirrorComponent;
  let fixture: ComponentFixture<CodeMirrorComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [CodeMirrorComponent]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CodeMirrorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

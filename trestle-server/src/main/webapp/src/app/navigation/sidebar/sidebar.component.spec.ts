import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SidebarComponent } from './sidebar.component';
import {Store} from '@ngrx/store';

describe('SidebarComponent', () => {
  let component: SidebarComponent;
  let fixture: ComponentFixture<SidebarComponent>;

  const testStore = jasmine.createSpyObj('Store', ['select']);

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SidebarComponent ],
      providers: [
        {
          provide: Store, useValue: testStore
        }
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SidebarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should should have no routes', () => {
    expect(component).toMatchSnapshot();
  });

  it('should have admin options', () => {
    expect(component).toMatchSnapshot();
  });

  it('should have dba options', () => {
    expect(component).toMatchSnapshot();
  })
});

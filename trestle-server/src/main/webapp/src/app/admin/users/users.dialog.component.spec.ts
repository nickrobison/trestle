import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import {UserDialogComponent} from './users.dialog.component';
import {MaterialModule} from '../../material/material.module';
import {UserModule} from '../../user/user.module';
import {ReactiveFormsModule} from '@angular/forms';
import {SharedModule} from '../../shared/shared.module';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {HttpClientTestingModule} from '@angular/common/http/testing';
import {MatDialogRef} from '@angular/material/dialog';
import {of} from 'rxjs';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';

class MatDialogMock {
  open() {
    return {
      afterClosed: () => of()
    };
  }
}

describe('UsersDialogComponent', () => {
  let component: UserDialogComponent;
  let fixture: ComponentFixture<UserDialogComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [MaterialModule, UserModule, ReactiveFormsModule, SharedModule, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{
        provide: MatDialogRef, useClass: MatDialogMock
      }],
      declarations: [UserDialogComponent],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(UserDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

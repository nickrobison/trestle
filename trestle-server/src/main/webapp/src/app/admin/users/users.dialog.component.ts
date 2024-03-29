/**
 * Created by nrobison on 2/6/17.
 */
import {Component, OnInit} from '@angular/core';
import {AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators} from '@angular/forms';
import {ITrestleUser} from '../../user/authentication.service';
import {UserService} from '../../user/users.service';
import {MatDialogRef} from '@angular/material/dialog';
import {Observable, of, timer} from 'rxjs';
import {map, switchMap} from 'rxjs/operators';
import {Privileges} from '../../user/trestle-user';

export enum UserDialogResponseType {
  ADD,
  DELETE
}

export interface IUserDialogResponse {
  type: UserDialogResponseType;
  user: ITrestleUser;
}

@Component({
  selector: 'user-add-dialog',
  templateUrl: './users.dialog.component.html',
  styleUrls: ['./users.dialog.component.scss'],
  providers: [UserService]
})
export class UserDialogComponent implements OnInit {
  public privileges: Map<string, number> = new Map();
  public user: ITrestleUser;
  public updateMode = true;
  public registerForm: FormGroup;
  public maxPasswordLength = 60;

  constructor(public dialogRef: MatDialogRef<UserDialogComponent>,
              private userService: UserService,
              private formBuilder: FormBuilder) {
//    Try to list all the enum keys
    for (const priv in Privileges) {
      if (parseInt(priv, 10) >= 0) {
        console.debug('Privs:', priv, Privileges[priv]);
        this.privileges.set(Privileges[priv], parseInt(priv, 10));
      }
    }
  }

  public ngOnInit(): void {
    // Merge the user object (which might be null) with a blank default
    const mergedUser = {
      ...{
        firstName: '',
        lastName: '',
        username: '',
        password: '',
        email: '',
        privileges: 1
      }, ...this.user
    };

    console.debug('Merged user:', mergedUser);
    //    Create the form
    this.registerForm = this.formBuilder.group({
      firstName: mergedUser.firstName,
      lastName: mergedUser.lastName,
      username: [mergedUser.username, undefined, this.validateUser],
      password: [mergedUser.password, this.validatePasswordLength],
      email: [mergedUser.email, Validators.email]
    });
    if (this.user == undefined || this.user.id === undefined) {
      this.updateMode = false;
      console.debug('Passed null user, creating blank instance');
      this.user = {
        username: '',
        password: '',
        firstName: '',
        lastName: '',
        email: '',
        privileges: 1
      };
    }
  }

  /**
   * Are we currently set to update the user?
   * @returns {boolean}
   */
  public isUpdate(): boolean {
    return this.updateMode;
  }

  /**
   * Save the new/updated user to the database
   */
  public save() {
    if (this.registerForm.valid) {
      // Construct a new user object
      const mergedUser = {
        ...this.user,
        ...this.registerForm.value
      };
      this.userService.modifyUser(mergedUser).subscribe(data => {
        console.debug('Response to add:', data);
        const responseID = parseInt(data, 10);
        if (!this.isUpdate()) {
          mergedUser.id = responseID;
        }
        this.dialogRef.close({
          type: UserDialogResponseType.ADD,
          user: mergedUser
        });
      }, (err: Error) => console.error(err));
    }
  }

  /**
   * Delete the specified user from the database
   */
  public delete() {
    if (this.user.id !== undefined) {
      this.userService
        .deleteUser(this.user.id)
        .subscribe(() => this.dialogRef.close({
            type: UserDialogResponseType.DELETE,
            user: this.user
          }),
          (err: Error) => console.error(err));
    } else {
      console.error('Tried to save a user, but the ID was null');
    }
  }

  /**
   * Dismiss modal, without saving any changes
   */
  public dismiss() {
    this.dialogRef.close(null);
  }

  /**
   * Change permission level of the user to the given values
   * @param {Privileges} level
   */
  public alterPermissionLevel(level: Privileges): void {
    // eslint-disable-next-line no-bitwise
    this.user.privileges = this.user.privileges ^ level;
  }

  /**
   * Does the user currently have the given permission?
   * @param {Privileges} privilage to check
   * @returns {boolean}
   */
  public isSelected(privilage: Privileges): boolean {
    // eslint-disable-next-line no-bitwise
    return (this.user.privileges & privilage) > 0;
  }

  public getEmailErrorMessage(): string {
    if (this.registerForm.controls['email'].hasError('required')) {
      return 'Value is required';
    }
    if (this.registerForm.controls['email'].hasError('email')) {
      return 'Not a valid email address';
    }
    return '';
  }

  public showLengthHint(value?: string): boolean {
    if (value) {
      if (this.isUpdate() && value.length === 60) {
        return false;
      }
      // Warn at 70% length
      if ((value.length / this.maxPasswordLength) > 0.7) {
        return true;
      }
    }

    return false;
  }

  public getPasswordErrorMessage(): string {
    if (this.registerForm.controls['password'].hasError('maxLength')) {
      return 'Password too long';
    }

    if (this.registerForm.controls['password'].hasError('required')) {
      return 'Value is required';
    }

    return '';
  }

  public getUsernameErrorMessage(): string {
    if (this.registerForm.controls['username'].hasError('required')) {
      return 'Value is required';
    }
    if (this.registerForm.controls['username'].hasError('userExists')) {
      return 'Username taken';
    }
    return '';
  }

  public validateUser = (c: AbstractControl): Observable<ValidationErrors | null> => {
    if (this.isUpdate() || c.value === null || c.value === '') {
      console.debug('Returning');
      return of(null);
    }
    return timer(500)
      .pipe(
        switchMap(() => {
          return this.userService.userExists(c.value)
            .pipe(
              map((exists) => {
                console.debug('User exists?', exists);
                return exists ? ({
                  userExists: {
                    value: c.value
                  }
                } as ValidationErrors) : null;
              }));
        }));
  };

  public validatePasswordLength = (c: AbstractControl): null | ValidationErrors => {
    if (c.value === null || c.value === '') {
      return null;
    }

    // If we're update and the password length is max, that's fine
    if (this.isUpdate() && c.value.length === this.maxPasswordLength) {
      return null;
    }

    //    Finally, validate it
    return c.value.length <= this.maxPasswordLength ? null : {
      maxLength: {
        value: true
      }
    };
  };
}

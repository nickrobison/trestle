/**
 * Created by nrobison on 1/19/17.
 */
import {Component, OnInit, ViewContainerRef} from '@angular/core';
import {IUserDialogResponse, UserDialogComponent, UserDialogResponseType} from './users.dialog.component';
import {ITrestleUser, Privileges} from '../../user/authentication.service';
import {MatDialog, MatDialogConfig, MatDialogRef} from '@angular/material/dialog';
import {UserService} from '../../user/users.service';

@Component({
  selector: 'admin-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss'],
})

export class UsersComponent implements OnInit {

  public users: ITrestleUser[];
  public dialogRef: MatDialogRef<any> | null;
  public privileges: Privileges;

  constructor(private userService: UserService,
              public dialog: MatDialog,
              public viewContainerRef: ViewContainerRef) {
  }

  public ngOnInit(): void {
    this.loadUsers();
  }

  /**
   * Load all registered users
   */
  public loadUsers() {
    this.userService.getUsers()
      .subscribe((users: ITrestleUser[]) => this.users = users,
        (err: any) => console.error(err));
  };

  /**
   * Is the user an admin?
   * @param {ITrestleUser} user
   * @returns {boolean}
   */
  public isAdmin(user: ITrestleUser): boolean {
    // tslint:disable-next-line:no-bitwise
    return (user.privileges & Privileges.ADMIN) > 0;
  }

  /**
   * Is the user a DBA?
   * @param {ITrestleUser} user
   * @returns {boolean}
   */
  public isDBA(user: ITrestleUser): boolean {
    // tslint:disable-next-line:no-bitwise
    return (user.privileges & Privileges.DBA) > 0;
  }

  /**
   * Open the user modal and pass in the selected user
   * When the modal closes, perform the required action against the database
   * @param {ITrestleUser | null} user
   */
  public openUserModal(user: ITrestleUser | null) {
    const config = new MatDialogConfig();
    config.viewContainerRef = this.viewContainerRef;
    this.dialogRef = this.dialog.open(UserDialogComponent, config);
    // Clone the user so that we don't modify properties in the original table
    this.dialogRef.componentInstance.user = {...user};
    this.dialogRef.afterClosed().subscribe((result: IUserDialogResponse) => {
      console.debug('Dialog closed');
      console.debug('Result:', result);
      if (result != null) {
        const userIdx = this.users.findIndex((tableUser) => tableUser.id === result.user.id);
        switch (result.type) {
          case UserDialogResponseType.ADD:
            console.debug('User idx:', userIdx);
            if (userIdx < 0) {
              this.users.push(result.user);
            } else {
              this.users[userIdx] = result.user;
              console.debug('Users:', this.users);
            }
            break;
          case UserDialogResponseType.DELETE:
            if (userIdx > -1) {
              console.debug('Splicing out user at location:', userIdx);
              this.users.splice(userIdx, 1);
            }
            break;
        }
      }
      this.dialogRef = null;
    });
  }
}

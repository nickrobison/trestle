/**
 * Created by nrobison on 1/19/17.
 */
import {Component, OnInit, ViewContainerRef} from "@angular/core";
import {UserAddDialog, IUserDialogResponse, UserDialogResponseType} from "./users.add.dialog";
import {AuthService, ITrestleUser, Privileges} from "../../UserModule/authentication.service";
import {UserService} from "../../UserModule/users.service";
import { MatDialog, MatDialogConfig, MatDialogRef } from "@angular/material";

@Component({
    selector: "admin-users",
    templateUrl: "./users.component.html",
    styleUrls: ["./users.component.css"],
})

export class UsersComponent implements OnInit {

    public users: ITrestleUser[];
    public dialogRef: MatDialogRef<any> | null;
    public privileges: Privileges;

    constructor(private userService: UserService, public dialog: MatDialog, public viewContainerRef: ViewContainerRef, public authService: AuthService) {
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
        this.dialogRef = this.dialog.open(UserAddDialog, config);
        this.dialogRef.componentInstance.user = user;
        this.dialogRef.afterClosed().subscribe((result: IUserDialogResponse) => {
            console.debug("Dialog closed");
            console.debug("Result:", result);
            if (result != null) {
                switch(result.type) {
                    case UserDialogResponseType.ADD:
                        if (!(this.users.filter(
                            (aUser) => aUser.id === result.user.id).length > 0)) {
                            this.users.push(result.user);
                        }
                        break;
                    case UserDialogResponseType.DELETE:
                        const index = this.users.indexOf(result.user);
                        console.debug("Splicing out user at location:", index);
                        if (index > -1) {
                            this.users.splice(index, 1);
                        }
                        break;
                }
            }
            this.dialogRef = null;
        });
    }
}

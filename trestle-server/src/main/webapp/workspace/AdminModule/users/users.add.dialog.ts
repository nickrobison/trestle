/**
 * Created by nrobison on 2/6/17.
 */
import {Component, OnInit} from "@angular/core";
import {Response} from "@angular/http";
import {ITrestleUser, Privileges} from "../../UserModule/authentication.service";
import {UserService} from "../../UserModule/users.service";
import { MatDialogRef } from "@angular/material";

export enum UserDialogResponseType {
    ADD,
    DELETE
}

export interface IUserDialogResponse {
    type: UserDialogResponseType;
    user: ITrestleUser;
}

@Component({
    selector: "user-add-dialog",
    templateUrl: "./users.add.dialog.html",
    providers: [UserService]
})
export class UserAddDialog implements OnInit {
    public privileges: Map<string, number> = new Map();
    public user: ITrestleUser;
    public updateMode = true;

    constructor(public dialogRef: MatDialogRef<UserAddDialog>, private userService: UserService) {
//    Try to list all the enum keys
        for (const priv in Privileges) {
            if (parseInt(priv, 10) >= 0) {
                console.debug("Privs:", priv, Privileges[priv]);
                this.privileges.set(Privileges[priv], parseInt(priv, 10));
            }
        }
    }

    public ngOnInit(): void {
        if (this.user == null) {
            this.updateMode = false;
            console.debug("Passed null user, creating blank instance");
            this.user = {
                username: "",
                password: "",
                firstName: "",
                lastName: "",
                email: "",
                privileges: 1
            };
        }
        console.debug("User to modify:", this.user);
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
        console.log("user:", this.user);
        this.userService.modifyUser(this.user).subscribe((data: Response) => {
            console.debug("Response to add:", data);
            const responseID = parseInt(data.text(), 10);
            if (!this.isUpdate()) {
                this.user.id = responseID;
            }
            this.dialogRef.close({
                type: UserDialogResponseType.ADD,
                user: this.user
            });
        }, (err: Error) => console.error(err));
    }

    /**
     * Delete the specified user from the database
     */
    public delete() {
        if (this.user.id !== undefined) {
            this.userService
                .deleteUser(this.user.id)
                .subscribe((data: any) => this.dialogRef.close({
                    type: UserDialogResponseType.DELETE,
                    user: this.user
                }),
                (err: Error) => console.error(err));
        } else {
            console.error("Tried to save a user, but the ID was null");
        }
    }

    /**
     * Change permission level of the user to the given values
     * @param {Privileges} level
     */
    public alterPermissionLevel(level: Privileges): void {
        // tslint:disable-next-line:no-bitwise
        this.user.privileges = this.user.privileges ^ level;
    }

    /**
     * Does the user currently have the given permission?
     * @param {Privileges} privilage to check
     * @returns {boolean}
     */
    public isSelected(privilage: Privileges): boolean {
        // tslint:disable-next-line:no-bitwise
        return (this.user.privileges & privilage) > 0;
    }
}

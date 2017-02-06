/**
 * Created by nrobison on 2/6/17.
 */
import {Component, OnInit} from "@angular/core";
import {ITrestleUser, Privileges} from "../../authentication.service";
import {MdDialogRef} from "@angular/material";
import {UserService} from "./users.service";
import {Response} from "@angular/http";


export enum UserDialogResponseType {
    ADD,
    DELETE
}

export interface IUserDialogResponse {
    type: UserDialogResponseType,
    user: ITrestleUser
}

@Component({
    selector: "user-add-dialog",
    templateUrl: "./users.add.dialog.html",
    providers: [UserService]
})
export class UserAddDialog implements OnInit {
    privileges: Map<string, number> = new Map();
    user: ITrestleUser;
    updateMode = true;

    constructor(public dialogRef: MdDialogRef<UserAddDialog>, private userService: UserService) {
//    Try to list all the enum keys
        for (let priv in Privileges) {
            if (parseInt(priv, 10) >= 0) {
                console.debug("Privs:", priv, Privileges[priv]);
                this.privileges.set(Privileges[priv], parseInt(priv, 10));
            }
        }
    }

    ngOnInit(): void {
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

    public isUpdate(): boolean {
        return this.updateMode;
    }

    public saveWithValid(model: ITrestleUser, isValid: boolean) {
        console.debug("user:", model);
    }

    public save() {
        console.log("user:", this.user);
        this.userService.modifyUser(this.user).subscribe((data: Response) => {
            console.debug("Response to add:", data);
            let responseID = parseInt(data.text(), 10);
            if (!this.isUpdate()) {
                this.user.id = responseID;
            }
            this.dialogRef.close({
                type: UserDialogResponseType.ADD,
                user: this.user
            }),
                (err: Error) => console.error(err)
        });

    }

    public delete() {
        this.userService.deleteUser(this.user.id).subscribe((data: any) => this.dialogRef.close({
            type: UserDialogResponseType.DELETE,
            user: this.user
            }),
            (err: Error) => console.error(err));
    }

    public alterPermissionLevel(level: Privileges): void {
        this.user.privileges = this.user.privileges ^ level;
        console.debug("User priv level:", this.user.privileges);
    }

    public isSelected(privilage: Privileges): boolean {
        return (this.user.privileges & privilage) > 0;
    }
}
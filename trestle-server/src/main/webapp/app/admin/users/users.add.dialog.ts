/**
 * Created by nrobison on 2/6/17.
 */
import {Component, OnInit} from "@angular/core";
import {ITrestleUser, Privileges} from "../../authentication.service";
import {MdDialogRef} from "@angular/material";

@Component({
    selector: "user-add-dialog",
    templateUrl: "./users.add.dialog.html"
})
export class UserAddDialog implements OnInit {
    privileges: Map<string, number> = new Map();
    user: ITrestleUser;

    constructor(public dialogRef: MdDialogRef<UserAddDialog>) {
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
    }

    public saveWithValid(model: ITrestleUser, isValid: boolean) {
        console.debug("user:", model);
    }

    public save() {
        console.log("user:", this.user);
        this.dialogRef.close(this.user);
    }

    public alterPermissionLevel(level: Privileges): void {
        this.user.privileges = this.user.privileges ^ level;
        console.debug("User priv level:", this.user.privileges);
    }

    public isSelected(privilage: Privileges): boolean {
        return (this.user.privileges & privilage) > 0;
    }
}
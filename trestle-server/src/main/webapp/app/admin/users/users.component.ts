/**
 * Created by nrobison on 1/19/17.
 */
import {Component, OnInit, ViewContainerRef, Pipe, PipeTransform} from "@angular/core";
import {UserService} from "./users.service";
import {ITrestleUser, Privileges} from "../../authentication.service";
import {MdDialogRef, MdDialog, MdDialogConfig} from "@angular/material";

@Component({
    selector: "admin-users",
    templateUrl: "./users.component.html",
    styleUrls: ["./users.component.css"],
})

export class UsersComponent implements OnInit {

    users: Array<ITrestleUser>;
    dialogRef: MdDialogRef<any>;

    constructor(private userService: UserService, public dialog: MdDialog, public viewContainerRef: ViewContainerRef) {
    }

    ngOnInit(): void {
        this.loadUsers();
    }

    public loadUsers() {
        this.userService.getUsers()
            .subscribe(users => this.users = users,
                err => console.error(err))
    };

    public openUserModal(user: ITrestleUser) {
        let config = new MdDialogConfig();
        config.viewContainerRef = this.viewContainerRef;
        this.dialogRef = this.dialog.open(UserAddDialog, config);
        this.dialogRef.componentInstance.user = user;
        this.dialogRef.afterClosed().subscribe((result) => {
            console.debug("Dialog closed");
            console.debug(result);
            this.dialogRef = null;
        })
    }
}

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

    public savewithValid(model: ITrestleUser, isValid: boolean) {
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

@Pipe({name: "mapValues"})
export class MapValuesPipe implements PipeTransform {
    transform(value: any, ...args: any[]): any {
        let returnArray: Array<any> = [];
        value.forEach((entryVal: any, entryKey: any) => {
            returnArray.push({
                key: entryKey,
                value: entryVal
            });
        });
        return returnArray;
    }
}
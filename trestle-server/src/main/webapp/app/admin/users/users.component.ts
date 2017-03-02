/**
 * Created by nrobison on 1/19/17.
 */
import {Component, OnInit, ViewContainerRef, Pipe, PipeTransform} from "@angular/core";
import {UserService} from "./users.service";
import {ITrestleUser, AuthService, Privileges} from "../../authentication.service";
import {MdDialogRef, MdDialog, MdDialogConfig} from "@angular/material";
import {UserAddDialog, IUserDialogResponse, UserDialogResponseType} from "./users.add.dialog";

@Component({
    selector: "admin-users",
    templateUrl: "./users.component.html",
    styleUrls: ["./users.component.css"],
})

export class UsersComponent implements OnInit {

    users: Array<ITrestleUser>;
    dialogRef: MdDialogRef<any>;
    privileges: Privileges;

    constructor(private userService: UserService, public dialog: MdDialog, public viewContainerRef: ViewContainerRef, public authService: AuthService) {
    }

    ngOnInit(): void {
        this.loadUsers();
    }

    public loadUsers() {
        this.userService.getUsers()
            .subscribe(users => this.users = users,
                err => console.error(err))
    };

    public isAdmin(user: ITrestleUser): boolean {
        return (user.privileges & Privileges.ADMIN) > 0;
    }

    public isDBA(user: ITrestleUser): boolean {
        return (user.privileges & Privileges.DBA) > 0;
    }

    public openUserModal(user: ITrestleUser) {
        let config = new MdDialogConfig();
        config.viewContainerRef = this.viewContainerRef;
        this.dialogRef = this.dialog.open(UserAddDialog, config);
        this.dialogRef.componentInstance.user = user;
        this.dialogRef.afterClosed().subscribe((result: IUserDialogResponse) => {
            console.debug("Dialog closed");
            console.debug("Result:", result);
            if (result != null) {
                switch(result.type) {
                    case UserDialogResponseType.ADD:
                        this.users.push(result.user);
                        break;
                    case UserDialogResponseType.DELETE:
                        let index = this.users.indexOf(result.user);
                        console.debug("Splicing out user at location:", index);
                        if (index > -1) {
                            this.users.splice(index, 1);
                        }
                        break;
                }
            }
            this.dialogRef = null;
        })
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
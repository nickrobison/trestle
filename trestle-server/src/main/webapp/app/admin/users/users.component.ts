/**
 * Created by nrobison on 1/19/17.
 */
import {Component, OnInit, ViewContainerRef, Pipe, PipeTransform} from "@angular/core";
import {UserService} from "./users.service";
import {ITrestleUser, Privileges} from "../../authentication.service";
import {MdDialogRef, MdDialog, MdDialogConfig} from "@angular/material";
import {UserAddDialog} from "./users.add.dialog";

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
        this.dialogRef.afterClosed().subscribe((result: ITrestleUser) => {
            console.debug("Dialog closed");
            if (result != null) {
                this.userService.modifyUser(result).subscribe((data: any) => {
                        console.debug("User saved");
                        this.users.push(result);
                    },
                        (err: Error) => console.error(err)
                );
            }
            // console.debug(result);
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
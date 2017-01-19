/**
 * Created by nrobison on 1/19/17.
 */
import {Component, OnInit} from "@angular/core";
import {UserService, ITrestleUser} from "./users.service";

@Component({
    selector: "admin-users",
    templateUrl: "./users.component.html",
    styleUrls: ["./users.component.css"],
})

export class UsersComponent implements OnInit {

    users: Array<ITrestleUser>;

    constructor(private userService: UserService) {
    }

    ngOnInit(): void {
        this.loadUsers();
    }

    public loadUsers() {
        this.userService.getUsers()
            .subscribe(users => this.users = users,
                err => console.error(err))
    };
}
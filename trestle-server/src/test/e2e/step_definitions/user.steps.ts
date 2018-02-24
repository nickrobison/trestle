import { binding, then, when } from "cucumber-tsflow";
import { expect } from "chai";
import { IUserTable, UserType } from "../page_objects/user.add.modal";
import { UsersPage } from "../page_objects/users.page";
import {element, by} from "protractor";

export interface CucumberTable<T> {
    hashes(): T[];
}


@binding()
export class UserSteps {

    private userPage = new UsersPage();

    @then(/^I create a new "([^"]*)" with the following properties:$/)
    public createUser(userType: UserType, userData: CucumberTable<IUserTable>) {
        console.debug("User", userData);
        return this.userPage.createUser(userType, userData.hashes()[0]);
    }

    @then(/^The users table should have (\d+) users$/)
    public verifyUserCount(userCount: number) {
        return expect(this.userPage.countUsers())
            .to.become(userCount);
    }

    @when(/^I delete user "([^"]*)"$/)
    public deleteUser(usename: string) {
        return this.userPage.deleteUser(usename);
    }
}
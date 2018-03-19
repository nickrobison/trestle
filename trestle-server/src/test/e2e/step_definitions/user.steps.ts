import { binding, then, when } from "cucumber-tsflow";
import { expect } from "chai";
import { IUserTable, UserType } from "../page_objects/user.details.modal";
import { UsersPage } from "../page_objects/users.page";

export interface CucumberTable<T> {
    hashes(): T[];
}


@binding()
export class UserSteps {

    private userPage = new UsersPage();

    @then(/^I create and submit a new "([^"]*)" with the following properties:$/)
    public async createAndSubmitUser(userType: UserType, userData: CucumberTable<IUserTable>) {
        await this.userPage.fillUserForm(userType, userData.hashes()[0]);
        return this.userPage.submitModal();
    }

    @then(/^I create a new "([^"]*)" with the following properties:$/)
    public createUser(userType: UserType, userData: CucumberTable<IUserTable>) {
        return this.userPage.fillUserForm(userType, userData.hashes()[0]);
    }

    @then(/^I edit user "([^"]*)" properties:$/)
    public editUser(user: string, userData: CucumberTable<IUserTable>) {
        return this.userPage.editUser(user, userData.hashes()[0]);
    }

    @then(/^The users table should have (\d+) users$/)
    public verifyUserCount(userCount: number) {
        return expect(this.userPage.countUsers())
            .to.become(userCount);
    }

    @when(/^I delete user "([^"]*)"$/)
    public deleteUser(username: string) {
        return this.userPage.deleteUser(username);
    }

    @then(/^Form field "([^"]*)" should have error "([^"]*)"$/)
    public formErrorMessage(field: string, message: string) {
        return expect(this.userPage.getFieldMessage(field))
            .to.become(message);
    }

    @then(/I dismiss the modal$/)
    public async dismissModal() {
        return this.userPage.dismissModal();
    }
}

import {expect} from 'chai';
import {UsersPage} from '../page_objects/users.page';
import {Then, When} from 'cucumber';

export interface CucumberTable<T> {
  hashes(): T[];
}

const userPage = new UsersPage();

Then(/^I create and submit a new "([^"]*)" with the following properties:$/, async (userType, userData) => {
  await userPage.fillUserForm(userType, userData.hashes()[0]);
  return userPage.submitModal();
});

Then(/^I create a new "([^"]*)" with the following properties:$/, async (userType, userData) => {
  return userPage.fillUserForm(userType, userData.hashes()[0]);
});

Then(/^I edit user "([^"]*)" properties:$/, async (user, userData) => {
  return userPage.editUser(user, userData.hashes()[0]);
});

Then(/^The users table should have (\d+) users$/, async (userCount) => {
  return expect(userPage.countUsers())
    .to.become(userCount);
});

When(/^I delete user "([^"]*)"$/, async (username) => {
  return userPage.deleteUser(username);
});

Then(/^Form field "([^"]*)" should have error "([^"]*)"$/, async (field, message) => {
  return expect(userPage.getFieldMessage(field))
    .to.become(message);
});

Then(/I dismiss the modal$/, async () => {
  return userPage.dismissModal();
});
//
//
// @binding()
// export class UserSteps {
//
//
//
//     @then(/^I create and submit a new "([^"]*)" with the following properties:$/)
//     public async createAndSubmitUser(userType: UserType, userData: CucumberTable<IUserTable>) {
//         await this.userPage.fillUserForm(userType, userData.hashes()[0]);
//         return this.userPage.submitModal();
//     }
//
//     @then(/^I create a new "([^"]*)" with the following properties:$/)
//     public createUser(userType: UserType, userData: CucumberTable<IUserTable>) {
//         return this.userPage.fillUserForm(userType, userData.hashes()[0]);
//     }
//
//     @then(/^I edit user "([^"]*)" properties:$/)
//     public editUser(user: string, userData: CucumberTable<IUserTable>) {
//         return this.userPage.editUser(user, userData.hashes()[0]);
//     }
//
//     @then(/^The users table should have (\d+) users$/)
//     public verifyUserCount(userCount: number) {
//         return expect(this.userPage.countUsers())
//             .to.become(userCount);
//     }
//
//     @when(/^I delete user "([^"]*)"$/)
//     public deleteUser(username: string) {
//         return this.userPage.deleteUser(username);
//     }
//
//     @then(/^Form field "([^"]*)" should have error "([^"]*)"$/)
//     public formErrorMessage(field: string, message: string) {
//         return expect(this.userPage.getFieldMessage(field))
//             .to.become(message);
//     }
//
//     @then(/I dismiss the modal$/)
//     public async dismissModal() {
//         return this.userPage.dismissModal();
//     }
// }

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

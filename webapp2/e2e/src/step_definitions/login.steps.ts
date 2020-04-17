/**
 * Created by nrobison on 5/31/17.
 */
import {Given, When, Then} from "cucumber";
import {DashboardPageObject} from '../page_objects/main.page';
import {LoginPageObject} from '../page_objects/login.page';
import * as chaiAsPromised from 'chai-as-promised';
import {by} from 'protractor';
import * as chai from 'chai';
const expect = chai.expect;

chai.use(chaiAsPromised);

const dashboard = new DashboardPageObject();
const login = new LoginPageObject();

Given(/^I login with (.*) and (.*)$/, async (username, password) => {
  return login.loginUser(username, password);
});

When(/^I login and submit with "([^"]*)" and "([^"]*)"$/, async (username, password) => {
  return login.loginUser(username, password, true);
});

Then(/^The login form is validated (.*)$/, async (valid) => {
      const isValid = valid === "true";
    return expect(login.formValidState(isValid))
        .to.become(true);
});

Then(/^The error message should be "([^"]*)"$/, async (message) => {
      return expect(login.getElementText(by.css("mat-card-footer")))
        .to.become(message);
});

Then(/^I logout$/, async () => {
  dashboard.clickButton("logout");
});

// private loginUser(username: string, password: string) {
//     return this.login.loginUser(username, password);
// }
//
// @when(/^I login and submit with "([^"]*)" and "([^"]*)"$/)
// private loginSubmit(username: string, password: string) {
//     return this.login.loginUser(username, password, true);
// }
//
// @then(/^The login form is validated (.*)$/)
// private formIsValid(valid: string) {
//     const isValid = valid === "true";
//     return expect(this.login.formValidState(isValid))
//         .to.become(true);
// }
//
// @then(/^The error message should be "([^"]*)"$/)
// private validateErrorMessage(message: string) {
//     return expect(this.login.getElementText(by.css("mat-card-footer")))
//         .to.become(message);
// }
//
// @then(/^I logout$/)
// public logout() {
//     return this.dashboard.clickButton("logout");
// }

// @binding()
// export class LoginSteps {






// }

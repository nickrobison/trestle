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

Given(/^I login with (.*) and (.*)$/, async (username: string | null, password: string | null) => {
  const u = username === null ? "" : username;
  const p = password === null ? "" : password;
  return login.loginUser(u, p);
});

When(/^I login and submit with "([^"]*)" and "([^"]*)"$/, async (username: string | null, password: string | null) => {
  const u = username === null ? "" : username;
  const p = password === null ? "" : password;
  return login.loginUser(u, p, true);
});

Then(/^The login form is validated (.*)$/, async (valid) => {
      const isValid = valid === "true";
    return expect(login.formValidState(isValid))
        .to.become(true);
});

Then(/^The error message should be "([^"]*)"$/, async (message) => {
      return expect(login.getElementText(by.id("mat-card-footer")))
        .to.become(message);
});

Then(/^I logout$/, async () => {
  return dashboard.clickButton("logout");
});

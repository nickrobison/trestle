import {DashboardPageObject} from '../page_objects/main.page';
import {Given, Then, When} from '@cucumber/cucumber';
import {browser} from 'protractor';
import {expect} from 'chai';

const dashboard = new DashboardPageObject();

Given(/^I am viewing the dashboard$/, async () => {
  return browser.get(browser.baseUrl);
});

Given(/^I am viewing the "([^"]*)" page$/, async (page) => {
  return dashboard.navigateToPage(page);
});

When(/^I click the "([^"]*)" button$/, async (button) => {
  return dashboard.clickButton(button);
});

Then(/^The "([^"]*)" page appears$/, async (page) => {
  // if we expect the dashboard, substitute for the root url
  await browser.sleep(500);
  const expectedUrl = browser.baseUrl + (page === 'dashboard' ? '' : page);
  return expect(browser.getCurrentUrl())
    .to.become(expectedUrl);
});

import {DashboardPageObject} from '../page_objects/main.page';
import {expect} from 'chai';
import {Then} from 'cucumber';
import {browser} from 'protractor';

const dashboard = new DashboardPageObject();

Then(/^I can see (\d+) "([^"]*)" options$/, async (optionNumber, optionType) => {
  return expect(dashboard.getPageActions(optionType))
    .to.become(optionNumber);
});

Then(/^I can navigate to "([^"]*)"$/, async (page) => {
  await browser.get(page);
  return expect(browser.getCurrentUrl())
    .to.become(browser.baseUrl + page);
});

Then(/^I can not navigate to "([^"]*)"$/, async (page) => {
  await browser.get(page);
  return expect(browser.getCurrentUrl())
    .to.not.become(browser.baseUrl + page);
});

// @binding()
// class PermissionSteps {
//
//
//
//     @then(/^I can see (\d+) "([^"]*)" options$/)
//     private validatePageOptions(optionNumber: number, optionType: PageActionType) {
//         return expect(this.dashboard.getPageActions(optionType))
//             .to.become(optionNumber);
//     }
//
//     @then(/^I can navigate to "([^"]*)"$/)
//     private async testCanNavigate(page: string) {
//         await browser.get(page);
//         return expect(browser.getCurrentUrl())
//             .to.become(browser.baseUrl + page);
//     }
//
//     @then(/^I can not navigate to "([^"]*)"$/)
//     private async testCanNotNavigate(page: string) {
//         await browser.get(page);
//         return expect(browser.getCurrentUrl())
//             .to.not.become(browser.baseUrl + page);
//     }
// }

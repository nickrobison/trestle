import { binding, then } from "cucumber-tsflow";
import { DashboardPageObject, PageActionType } from "../page_objects/main.page";
import { expect } from "chai";
import { browser } from "protractor";

@binding()
class PermissionSteps {

    private dashboard = new DashboardPageObject();

    @then(/^I can see (\d+) "([^"]*)" options$/)
    private validatePageOptions(optionNumber: number, optionType: PageActionType) {
        return expect(this.dashboard.getPageActions(optionType))
            .to.become(optionNumber);
    }

    @then(/^I can navigate to "([^"]*)"$/)
    private async testCanNavigate(page: string) {
        await browser.get(page);
        return expect(browser.getCurrentUrl())
            .to.become(browser.baseUrl + page);
    }

    @then(/^I can not navigate to "([^"]*)"$/)
    private async testCanNotNavigate(page: string) {
        await browser.get(page);
        return expect(browser.getCurrentUrl())
            .to.not.become(browser.baseUrl + page);
    }
}

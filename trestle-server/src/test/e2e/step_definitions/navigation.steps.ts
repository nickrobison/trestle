import { binding, given, then, when } from "cucumber-tsflow";
import { DashboardPageObject } from "../page_objects/main.page";
import { browser } from "protractor";
import { expect } from "chai";

@binding()
export class NavigationSteps {

    private dashboard: DashboardPageObject;

    public constructor() {
        this.dashboard = new DashboardPageObject();
    }

    @given(/^I am viewing the dashboard$/)
    private viewDashboard() {
        return browser.get(browser.baseUrl);
        // return this.dashboard.navigateToPage(page);
    }

    @given(/^I am viewing the "([^"]*)" page$/)
    private viewPage(page: string) {
        return this.dashboard.navigateToPage(page);
    }

    @when(/^I click the "([^"]*)" button$/)
    private clickButton(button: string) {
        return this.dashboard.clickButton(button);
    }

    @then(/^The "([^"]*)" page appears$/)
    private async validatePageAppears(page: string) {
        // if we expect the dashboard, substitute for the root url
        await browser.sleep(500);
        const expectedUrl = browser.baseUrl + (page === "dashboard" ? "" : page);
        return expect(browser.getCurrentUrl())
            .to.become(expectedUrl);
    }
}

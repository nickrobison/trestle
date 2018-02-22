/**
 * Created by nrobison on 5/31/17.
 */
import { browser, element, by, ExpectedConditions } from "protractor";

export type PageActionType = "admin" | "dba";

export class DashboardPageObject {

    constructor() {
    }

    public async goToLoginPage() {
        return browser.get("/login/");
    }

    public async navigateToPage(page: string) {
        return browser.get(page);
        // return browser.sleep(3000);
    }

    public async clickButton(button: string) {
        const buttonElement = element(by.id(button));
        const until = ExpectedConditions;
        await browser.wait(until.presenceOf(buttonElement), 5000, "Cannot click button");
        return buttonElement.click();
    }

    public async getPageActions(optionType: PageActionType): Promise<number> {
        const adminDiv = await element(by.id(optionType + "-actions"));
        return adminDiv.all(by.tagName("a")).count();
    }
}

/**
 * Created by nrobison on 5/31/17.
 */
import { browser, by, element } from "protractor";

export type PageActionType = "admin" | "dba";

export class DashboardPageObject {

    constructor() {
    }

    public async goToLoginPage() {
        return browser.get("/login/");
    }

    public async navigateToPage(page: string) {
        return browser.get(page);
    }

    public async clickButton(button: string) {
        return element(by.id(button)).click();
    }

    public async getPageActions(optionType: PageActionType): Promise<number> {
        const adminDiv = await element(by.id(optionType + "-actions"));
        return adminDiv.all(by.tagName("a")).count();
    }
}

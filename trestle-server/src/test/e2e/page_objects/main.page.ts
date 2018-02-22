/**
 * Created by nrobison on 5/31/17.
 */
import { browser, element, by } from "protractor";

export type PageActionType = "admin" | "dba";

export class DashboardPageObject {

    private pages: { [key: string]: string } = {
        dashboard: "",
        login: "login/"
    };

    constructor() {
    }

    public async goToLoginPage() {
        return browser.get("/login/");
    }

    public async navigateToPage(page: string) {
        browser.get(`${this.pages[page]}`);
        return browser.sleep(3000);
    }

    public async clickButton(button: string) {
        return element(by.id(button)).click();
    }

    public async getPageActions(optionType: PageActionType): Promise<number> {
        const adminDiv = await element(by.id(optionType + "-actions"));
        return adminDiv.all(by.tagName("a")).count();
    }
}

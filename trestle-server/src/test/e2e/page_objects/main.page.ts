/**
 * Created by nrobison on 5/31/17.
 */
import {browser, element, by} from "protractor";

export class DashboardPageObject {

    private pages: {[key: string]: string} = {
        "dashboard": "",
        "login": "login/"
    };

    constructor() {
    }


    async goToLoginPage() {
        return browser.get("/login/");
    }

    async navigateToPage(page: string) {
        browser.get(`${this.pages[page]}`);
        return browser.sleep(3000);
    }

    async clickButton(button: string) {
        return element(by.id(button)).click();
    }
}
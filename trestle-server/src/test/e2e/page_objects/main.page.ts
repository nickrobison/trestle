/**
 * Created by nrobison on 5/31/17.
 */
import {browser, element, by} from "protractor";

export class DashboardPageObject {

    private pages = {
        "home": "/workspace/",
        "login": "/login/"
    };

    constructor() {
    }


    async goToLoginPage() {
        return browser.get("/login/");
    }

    async navigateToPage(page: string) {
        return browser.get(`${this.pages["home"]}`);
    }

    async clickButton(button: string) {
        return element(by.id(button)).click();
    }
}
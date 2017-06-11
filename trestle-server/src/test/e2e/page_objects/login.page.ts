/**
 * Created by nrobison on 5/31/17.
 */
import { element, by, ElementFinder, browser } from 'protractor';

export class LoginPageObject {

    private header: ElementFinder;
    private usernameField: ElementFinder;
    private passwordField: ElementFinder;

    constructor() {
        this.header = element(by.css("md-card-header"));
        this.usernameField = element(by.css('input[formControlName="username"]'));
        this.passwordField = element(by.css("input[formControlName='password']"));
    }

    async pageIsValid(): Promise<boolean> {
        return Promise.resolve(false);
    }

    async getTitle() {
        return this.header.getText();
    }

    async loginUser(username: string, password: string) {
        console.log("Logging in", username, password);
        this.usernameField.sendKeys(username);
        this.passwordField.sendKeys(password);
        return browser.sleep(1000);
    }

    async isValid() {
        await browser.sleep(1000);
        return element(by.css("form .ng-valid")).isPresent();
    }

    private static fromControlName(value: any, opt_parentElement: any, opt_rootSelector: any) {
        let using = opt_parentElement || document;

        return using.querySelectorAll("[formControlName=" + value + "]");
    }
}
/**
 * Created by nrobison on 5/31/17.
 */
import { element, by, ElementFinder } from 'protractor';

export class LoginPageObject {

    private header: ElementFinder;
    private usernameField: ElementFinder;
    private passwordField: ElementFinder;
    private formValid: ElementFinder;

    constructor() {
        by.addLocator("formControlLocator", LoginPageObject.fromControlName);
        this.header = element(by.css("md-card-header"));
        this.usernameField = element(by.css('input[ng-reflect-name="username"]'));
        // this.passwordField = element(by.formControlLocator("password"));
        this.passwordField = element(by.css("input[formControl='password']"));
        this.formValid = element(by.css(".ng-valid"));

    }

    async pageIsValid(): Promise<boolean> {
        return Promise.resolve(false);
    }

    async getTitle() {
        return this.header.getText();
    }

    async loginUser(username: string, password: string) {
        this.usernameField.sendKeys(username);
        this.passwordField.sendKeys(password);
    }

    async isValid() {
        return this.formValid.isPresent();
    }

    private static fromControlName(value: any, opt_parentElement: any, opt_rootSelector: any) {
        let using = opt_parentElement || document;

        return using.querySelectorAll("[formControlName=" + value + "]");
    }
}
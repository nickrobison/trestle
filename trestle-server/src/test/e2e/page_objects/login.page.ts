/**
 * Created by nrobison on 5/31/17.
 */
import { browser, by, element, ElementFinder, Locator } from "protractor";

export class LoginPageObject {

    private header: ElementFinder;
    private usernameField: ElementFinder;
    private passwordField: ElementFinder;

    constructor() {
        this.header = element(by.css("mat-card-header"));
        this.usernameField = element(by.css('input[formcontrolname="username"]'));
        this.passwordField = element(by.css("input[formcontrolname='password']"));
    }

    public async getTitle() {
        return this.header.getText();
    }

    /**
     * Fill in user login details, and optionally submit them
     * @param {string} username - Username to fill in
     * @param {string} password - Password to fill in
     * @param {boolean} login - Should we login the user?
     * @returns {Promise<void>}
     */
    public async loginUser(username: string, password: string, login?: boolean): Promise<void> {
        console.log("Logging in", username, password);
        this.usernameField.sendKeys(username);
        this.passwordField.sendKeys(password);
        if (login) {
            return element(by.buttonText("Login")).click();
        }
        return browser.sleep(1000);
    }

    /**
     * Determines if the form matches the given valid state
     *
     * @param {boolean} valid - Should the form be value?
     * @returns {Promise<boolean>} - The form matches the given valid state
     */
    public async formValidState(valid: boolean): Promise<boolean> {
        const formSelector = by.css("form");
        if (valid) {
            return this.elementHasClass(formSelector, "ng-valid");
        }
        return this.elementHasClass(formSelector, "ng-invalid");
    }

    /**
     * Returns the text for the given element
     * @param {Locator} selector - Selector to use
     * @returns {Promise<string>} - Element text
     */
    public getElementText(selector: Locator): Promise<string> {
        return element(selector).getText();
    }

    /**
     * * Determines if the element for the given selector contains the specified class
     * @param {Locator} selector - selector to use
     * @param {string} clazz - String class name
     * @returns {Promise<boolean>} - The element contains the specified class
     */
    private async elementHasClass(selector: Locator, clazz: string): Promise<boolean> {
        return element(selector).getAttribute("class")
            .then((classes) => {
                return classes.split(" ").indexOf(clazz) !== -1;
            });
    }

    private static fromControlName(value: any, opt_parentElement: any, opt_rootSelector: any) {
        const using = opt_parentElement || document;

        return using.querySelectorAll("[formControlName=" + value + "]");
    }
}
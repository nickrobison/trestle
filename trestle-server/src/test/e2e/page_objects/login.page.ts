/**
 * Created by nrobison on 5/31/17.
 */
import { element, by, ElementFinder } from 'protractor';

export class LoginPageObject {

    private header: ElementFinder;

    constructor() {
        this.header = element(by.css("md-card-header"));
    }

    async pageIsValid(): Promise<boolean> {
        return Promise.resolve(false);
    }

    async getTitle() {
        return this.header.getText();
    }
}
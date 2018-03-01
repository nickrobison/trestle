/**
 * Created by nrobison on 5/31/17.
 */
import { binding, given, then, when } from "cucumber-tsflow";
import { DashboardPageObject } from "../page_objects/main.page";
import { LoginPageObject } from "../page_objects/login.page";
import { by } from "protractor";
import { expect } from "chai";
import * as chai from "chai";
import * as chaiAsPromised from "chai-as-promised";

chai.use(chaiAsPromised);

@binding()
export class LoginSteps {

    private dashboard = new DashboardPageObject();
    private login = new LoginPageObject();

    @given(/^I login with (.*) and (.*)$/)
    private loginUser(username: string, password: string) {
        return this.login.loginUser(username, password);
    }

    @when(/^I login and submit with "([^"]*)" and "([^"]*)"$/)
    private loginSubmit(username: string, password: string) {
        return this.login.loginUser(username, password, true);
    }

    @then(/^The login form is validated (.*)$/)
    private formIsValid(valid: string) {
        const isValid = valid === "true";
        return expect(this.login.formValidState(isValid))
            .to.become(true);
    }

    @then(/^The error message should be "([^"]*)"$/)
    private validateErrorMessage(message: string) {
        return expect(this.login.getElementText(by.css("mat-card-footer")))
            .to.become(message);
    }

    @then(/^I logout$/)
    public logout() {
        return this.dashboard.clickButton("logout");
    }
}

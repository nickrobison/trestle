/**
 * Created by nrobison on 5/31/17.
 */
import { binding, given, then, when } from "cucumber-tsflow";
import { DashboardPageObject } from "../page_objects/main.page";
import { LoginPageObject } from "../page_objects/login.page";
import { by } from "protractor";

const chai = require("chai").use(require("chai-as-promised"));
const expect = chai.expect;

@binding()
class LoginSteps {

    private dashboard = new DashboardPageObject();
    private login = new LoginPageObject();

    @given(/^I am viewing the dashboard$/)
    private viewDashboard() {
        return this.dashboard.navigateToPage("dashboard");
    }

    @given(/^I am viewing the "([^"]*)" page$/)
    private viewPage(page: string) {
        return this.dashboard.navigateToPage(page);
    }

    @then(/^Login page appears$/)
    private loginValid() {
        return expect(this.login.getTitle()).to.become("Login to Trestle");
    }

    @when(/^I click the "([^"]*)" button$/)
    private clickButton(button: string) {
        return this.dashboard.clickButton(button);
    }

    @given(/^I login with (.*) and (.*)$/)
    private loginUser(username: string, password: string) {
        return this.login.loginUser(username, password);
    }

    @when(/^I login and submit with "([^"]*)" and "([^"]*)"$/)
    private loginSubmit(username: sring, password: string) {
        return this.login.loginUser(username, password, true);
    }

    @then(/^The login form is validated (.*)$/)
    private formIsValid(valid: string) {
        const isValid = valid === "true";
        console.log("Form should be valid?", isValid);
        return expect(this.login.formValidState(isValid))
            .to.become(true);
    }

    @then(/^The error message should be "([^"]*)"$/)
    private validateErrorMessage(message: string) {
        return expect(this.login.getElementText(by.css("mat-card-footer")))
            .to.become(message);
    }
}

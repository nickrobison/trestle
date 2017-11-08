var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/**
 * Created by nrobison on 5/31/17.
 */
import { binding, given, then, when } from "cucumber-tsflow";
import { DashboardPageObject } from "../page_objects/main.page";
import { LoginPageObject } from "../page_objects/login.page";
var chai = require('chai').use(require('chai-as-promised'));
var expect = chai.expect;
var LoginSteps = (function () {
    function LoginSteps() {
        this.dashboard = new DashboardPageObject();
        this.login = new LoginPageObject();
    }
    LoginSteps.prototype.viewDashboard = function () {
        return this.dashboard.navigateToPage("dashboard");
    };
    LoginSteps.prototype.viewPage = function (page) {
        return this.dashboard.navigateToPage(page);
    };
    LoginSteps.prototype.loginValid = function () {
        return expect(this.login.getTitle()).to.become("Login to Trestle");
    };
    LoginSteps.prototype.clickButton = function (button) {
        return this.dashboard.clickButton(button);
    };
    LoginSteps.prototype.loginUser = function (username, password) {
        return this.login.loginUser(username, password);
    };
    LoginSteps.prototype.formIsValid = function (valid) {
        var isValid = valid === "true";
        console.log("Form should be valid?", isValid);
        return expect(this.login.isValid()).to.become(isValid);
    };
    return LoginSteps;
}());
__decorate([
    given(/^I am viewing the dashboard$/),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", void 0)
], LoginSteps.prototype, "viewDashboard", null);
__decorate([
    given(/^I am viewing the "([^"]*)" page$/),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], LoginSteps.prototype, "viewPage", null);
__decorate([
    then(/^Login page appears$/),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", void 0)
], LoginSteps.prototype, "loginValid", null);
__decorate([
    when(/^I click the "([^"]*)" button$/),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], LoginSteps.prototype, "clickButton", null);
__decorate([
    given(/^I login with (.*) and (.*)$/),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String]),
    __metadata("design:returntype", void 0)
], LoginSteps.prototype, "loginUser", null);
__decorate([
    then(/^The login form is validated (.*)$/),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], LoginSteps.prototype, "formIsValid", null);
LoginSteps = __decorate([
    binding()
], LoginSteps);
module.exports = LoginSteps;
//# sourceMappingURL=login.steps.js.map
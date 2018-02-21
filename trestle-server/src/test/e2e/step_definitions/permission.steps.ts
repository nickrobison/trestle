import { binding, then } from "cucumber-tsflow";
import { DashboardPageObject } from "../page_objects/main.page";
import { expect } from "chai";

@binding()
class PermissionSteps {

    private dashboard = new DashboardPageObject();

    @then(/^I can see (\d+) "([^"]*)" options$/)
    private validatePageOptions(optionNumber: number, optionType: string) {
        return expect(this.dashboard.getPageOptions(1))
            .to.become(optionNumber);
    }
}

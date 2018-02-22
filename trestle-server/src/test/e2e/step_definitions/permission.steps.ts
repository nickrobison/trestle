import { binding, then } from "cucumber-tsflow";
import { DashboardPageObject, PageActionType } from "../page_objects/main.page";
import { expect } from "chai";

@binding()
class PermissionSteps {

    private dashboard = new DashboardPageObject();

    @then(/^I can see (\d+) "([^"]*)" options$/)
    private validatePageOptions(optionNumber: number, optionType: PageActionType) {
        return expect(this.dashboard.getPageActions(optionType))
            .to.become(optionNumber);
    }
}

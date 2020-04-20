/**
 * Created by nrobison on 5/31/17.
 */
import { browser, by, element } from "protractor";

export type PageActionType = "admin" | "dba";

export class DashboardPageObject {

    constructor() {
    }

    public async goToLoginPage() {
        return browser.get("/login/");
    }

    public async navigateToPage(page: string) {
        return element(by.css("a[id='" + page + "']")).click();
    }

    public async clickButton(button: string) {
        await element(by.id(button)).click();
        if (button == "logout") {
          // TODO(nickrobison): Remove with TRESTLE-736
          return browser.refresh();
        }
        return;
    }

    public async getPageActions(optionType: PageActionType): Promise<number> {
        return element(by.id(optionType + "-actions")).all(by.tagName("a")).count();
    }
}

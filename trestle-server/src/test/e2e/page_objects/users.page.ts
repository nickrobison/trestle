import { browser, by, element } from "protractor";
import { IUserTable, UserDetailsModal, UserType } from "./user.details.modal";

export class UsersPage {

    private userModal = new UserDetailsModal();

    public async createUser(userType: UserType, user: IUserTable) {
        //    Click the button
        await element(by.id("add-user")).click();
        return this.userModal.createUser(userType, user);
    }

    public async countUsers(): Promise<number> {
        await browser.sleep(1000);
        return element(by.id("users-table"))
            .all(by.css("tbody tr")).count();
    }

    public async deleteUser(username: string) {
        const xpathString = "//table[@id='users-table']/tbody/tr[td//text()[contains(.,'"
            + username
            + "')]]";
        await element(by.xpath(xpathString)).click();
        // Wait 500ms for the modal to open
        await browser.sleep(500);

        return this.userModal.deleteUser();
        // const userRows = await element(by.id("users-table"))
        //     .all(by.css("tbody tr"));
        //
        // for (const row of userRows) {
        //     const cols = await row.all(by.css("td"));
        //     const colText = await cols[2].getText();
        //     console.log("Text:", colText);
        //     if (colText === username) {
        //         console.log("Found");
        //         await row.click();
        //         return this.userModal.deleteUser();
        //     }
        // }
        // return Promise.reject("Cannot find user!");
    }
}

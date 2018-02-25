import { browser, by, element } from "protractor";
import { IUserTable, UserAddModal, UserType } from "./user.add.modal";

export class UsersPage {

    private userModal = new UserAddModal();

    public async createUser(userType: UserType, user: IUserTable) {
        //    Click the button
        await browser.sleep(500);
        await element(by.id("add-user")).click();
        await this.userModal.createUser(userType, user);
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
        await browser.sleep(500);
        const useRow = element(by.xpath(xpathString));
        return useRow.click();
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

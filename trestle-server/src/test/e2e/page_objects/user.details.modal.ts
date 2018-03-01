import { browser, by, element, ElementFinder } from "protractor";

export interface IUserTable {
    username?: string,
    password?: string,
    first_name?: string,
    last_name?: string,
    email?: string;
}

export type UserType = "user" | "admin" | "dba";

export class UserDetailsModal {

    private userNameInput: ElementFinder;
    private passwordInput: ElementFinder;
    private firstNameInput: ElementFinder;
    private lastNameInput: ElementFinder;
    private emailInput: ElementFinder;

    private userPermission: ElementFinder;
    private adminPermission: ElementFinder;
    private dbaPermission: ElementFinder;

    public constructor() {
        this.userNameInput = element(by.id("username"));
        this.passwordInput = element(by.id("password"));
        this.firstNameInput = element(by.id("first_name"));
        this.lastNameInput = element(by.id("last_name"));
        this.emailInput = element(by.id("email"));

        this.userPermission = element(by.id("mat-button-toggle-0"));
        this.adminPermission = element(by.id("mat-button-toggle-1"));
        this.dbaPermission = element(by.id("mat-button-toggle-2"));
    }

    /**
     * Create a new user of the given type with the provided data
     * Assumes the modal is already open, but closes it in order to submit the data
     *
     * @param {UserType} userType - user type to add
     * @param {IUserTable} user - User data
     * @returns {Promise<any>} - Returns when the modal closes (waits 500ms)
     */
    public async createUser(userType: UserType, user: IUserTable) {
        // Wait for the modal to load
        await browser.sleep(500);
        await this.userNameInput.sendKeys(user.username || "");
        await this.passwordInput.sendKeys(user.password || "");
        await this.firstNameInput.sendKeys(user.first_name || "");
        await this.lastNameInput.sendKeys(user.last_name || "");
        await this.emailInput.sendKeys(user.email || "");

        //    Now the permissions, select everything lower
        switch (userType) {
            case "dba": {
                await this.dbaPermission.click();
                await this.adminPermission.click();
                await this.userPermission.click();
                break;
            }
            case "admin": {
                await this.adminPermission.click();
                await this.userPermission.click();
                break;
            }
            case "user": {
                await this.userPermission.click();
                break;
            }
        }

        await element(by.buttonText("Add User")).click();
        // Sleep for 500 ms to let the modal close
        return browser.sleep(500);
    }

    /**
     * Update the given properties of the user
     * Assumes the modal is already open, but closes it in order to submit the changes
     *
     * @param {IUserTable} user - User data to update
     * @returns {Promise<any>} - returns when the modal is closed (waits 500ms)
     */
    public async editUser(user: IUserTable) {
        // Wait for the modal to load
        await browser.sleep(500);
        // We have to clear the input fields before
        if (user.username) {
            await this.userNameInput.clear();
            await this.userNameInput.sendKeys(user.username);
        }

        if (user.email) {
            await this.emailInput.clear();
            await this.emailInput.sendKeys(user.email);
        }

        if (user.password) {
            await this.passwordInput.clear();
            await this.passwordInput.sendKeys(user.password);
        }

        if (user.first_name) {
            await this.firstNameInput.clear();
            await this.firstNameInput.sendKeys(user.first_name);
        }

        if (user.last_name) {
            await this.lastNameInput.clear();
            await this.lastNameInput.sendKeys(user.last_name);
        }

        element(by.buttonText("Update User")).click();
        // Sleep for 500 ms to let the modal close
        return browser.sleep(500);
    }

    /**
     * Delete the given user
     * Assumes the modal is already open, but closes it in order to submit the changes
     *
     * @returns {any} - Returns when the modal closes
     */
    public async deleteUser() {
        // Wait for the modal to load
        await browser.sleep(500);
        return element(by.buttonText("Delete User")).click();
    }
}

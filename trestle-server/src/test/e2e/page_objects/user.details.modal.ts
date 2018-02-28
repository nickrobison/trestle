import { browser, by, element, ElementFinder } from "protractor";

export interface IUserTable {
    username: string,
    password: string,
    first_name: string,
    last_name: string,
    email: string;
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
        this.adminPermission = element(by.id("mat-button-toggle-1"))
        this.dbaPermission = element(by.id("mat-button-toggle-2"));
    }

    public async createUser(userType: UserType, user: IUserTable) {
        await this.userNameInput.sendKeys(user.username);
        await this.passwordInput.sendKeys(user.password);
        await this.firstNameInput.sendKeys(user.first_name);
        await this.lastNameInput.sendKeys(user.last_name);
        await this.emailInput.sendKeys(user.email);

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

    public deleteUser() {
        return element(by.buttonText("Delete User")).click();
    }

}
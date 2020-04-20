import {browser, by, element} from 'protractor';
import {IUserTable, UserDetailsModal, UserType} from './user.details.modal';

export class UsersPage {

  private userModal = new UserDetailsModal();

  public async fillUserForm(userType: UserType, user: IUserTable) {
    //    Click the button
    await element(by.id('add-user')).click();
    return this.userModal.createUser(userType, user);
  }

  public async editUser(username: string, userData: IUserTable) {
    await UsersPage.selectUserRow(username);
    return this.userModal.editUser(userData);
  }

  public async countUsers(): Promise<number> {
    await browser.sleep(1000);
    return element(by.id('users-table'))
      .all(by.css('tbody tr')).count();
  }

  public async deleteUser(username: string) {
    await UsersPage.selectUserRow(username);
    return this.userModal.deleteUser();
  }

  public async submitModal() {
    return this.userModal.submit();
  }

  public dismissModal() {
    return this.userModal.dismiss();
  }

  public async getFieldMessage(field: string) {
    await browser.sleep(500);
    const xpathString = '//form//mat-form-field[.//input[@formcontrolname=\'' + field + '\']]//mat-error';
    const matField = element(by.xpath(xpathString));
    return matField.getText();
  }

  private static async selectUserRow(username: string) {
    const xpathString = '//table[@id=\'users-table\']/tbody/tr[td//text()[contains(.,\''
      + username
      + '\')]]';
    const rowElement = element(by.xpath(xpathString));
    // We need to use the JS click method, otherwise Firefox complains it can't scroll to the table row
    await browser.executeScript('arguments[0].click()', rowElement);
    // Wait 500ms for the modal to open
    return browser.sleep(500);
  }
}

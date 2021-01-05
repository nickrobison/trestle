import {ITrestleUser} from './authentication.service';

export enum Privileges {
  USER = 1,
  ADMIN = 2,
  DBA = 4
}

export class TrestleUser {
    private readonly _id: number | undefined;
    private readonly _firstName: string;
    private readonly _lastName: string;
    private _username: string;
    private readonly _email: string;
    private readonly _password: string;
    private readonly _privileges: number;

    public constructor(userInterface: ITrestleUser) {
        this._id = userInterface.id;
        this._email = userInterface.email;
        this._firstName = userInterface.firstName;
        this._lastName = userInterface.lastName;
        this._email = userInterface.email;
        this._password = userInterface.password;
        this._privileges = userInterface.privileges;
    }


    get id(): number | undefined {
        return this._id;
    }

    get firstName(): string {
        return this._firstName;
    }

    get lastName(): string {
        return this._lastName;
    }

    get username(): string {
        return this._username;
    }

    get email(): string {
        return this._email;
    }

    get password(): string {
        return this._password;
    }

    get privileges(): number {
        return this._privileges;
    }

    /**
     * Does the user have admin privileges?
     * This will return true for any permission level equal to or higher than admin (e.g. dba)
     * @returns {boolean} - Has admin or higher permissions?
     */
    public isAdmin(): boolean {
        // eslint-disable-next-line no-bitwise
        return (this._privileges & Privileges.ADMIN) > 0;
    }

    public hasRequiredPrivileges(roles: Privileges[]): boolean {
        // eslint-disable-next-line no-bitwise
        return (this._privileges & TrestleUser.buildRoleValue(roles)) > 0;
    }

    public serialize(): ITrestleUser {
      return {
        email: this._email,
        firstName: this._firstName,
        id: this._id,
        lastName: this._lastName,
        password: this._password,
        privileges: this._privileges,
        username: this._username
      }
    }

    /**
     * Convert an array of privileges into a single value
     * @param {Privileges[]} roles
     * @returns {number}
     */
    private static buildRoleValue(roles: Privileges[]): number {
        let roleValue = 0;
        roles.forEach((role) => {
            // eslint-disable-next-line no-bitwise
            roleValue = roleValue | role;
        });
        return roleValue;
    }
}

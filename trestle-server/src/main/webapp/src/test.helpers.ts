import {Privileges, TrestleUser} from './app/user/trestle-user';

export const createMockUser = (role: Privileges): TrestleUser => {
  return new TrestleUser({
    username: 'test',
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    privileges: role
  });
};

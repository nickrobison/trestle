import * as fromAuth from './auth.actions';

describe('loadAuths', () => {
  it('should return an action', () => {
    expect(fromAuth.login({username: "hello", password: "password"}).type).toBe('[Auth] Load Auths');
  });
});

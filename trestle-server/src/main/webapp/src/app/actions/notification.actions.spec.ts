import * as fromNotification from './notification.actions';

describe('loadNotifications', () => {
  it('should return an action', () => {
    expect(fromNotification.loadNotifications().type).toBe('[Notification] Load Notifications');
  });
});

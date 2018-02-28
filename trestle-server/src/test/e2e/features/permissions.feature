# Created by nickrobison at 2/21/18
@Permissions
Feature: User permissions feature
  Validates that users can only see the parts of the application they're authorized to

  Scenario: DBA permissions
    When I click the "login" button
    Then The "login" page appears
    When I login and submit with "dba" and "dba"
    Then I can see 3 "admin" options
    And I can see 1 "dba" options
    Then I can navigate to "admin/users"
    And I can navigate to "admin/index"
    Then I click the "logout" button

  Scenario: Admin permissions
    When I click the "login" button
    Then The "login" page appears
    When I login and submit with "admin" and "admin1"
    Then I can see 3 "admin" options
    And I can see 0 "dba" options
    Then I can navigate to "admin/users"
    And I can not navigate to "admin/index"
    Then I click the "logout" button

  Scenario: Admin permissions
    When I click the "login" button
    Then The "login" page appears
    When I login and submit with "user" and "user1"
    Then I can see 0 "admin" options
    And I can see 0 "dba" options
    Then I can not navigate to "users"
    And I can not navigate to "index"


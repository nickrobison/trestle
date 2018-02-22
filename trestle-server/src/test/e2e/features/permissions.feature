# Created by nickrobison at 2/21/18
Feature: User permissions feature
  Validates that users can only see the parts of the application they're authorized to

  @Permissions
  Scenario Outline: DBA permissions
    Given I am viewing the dashboard
    When I click the "login" button
    Then Login page appears
    When I login and submit with "<username>" and "<password>"
    Then I can see <adminActions> "admin" options
    And I can see <dbaActions> "dba" options
    Then I click the "logout" button

    Examples:
      | username | password | adminActions | dbaActions |
      | dba      | dba      | 3            | 1          |
      | admin    | admin1   | 3            | 0          |
      | user     | user1    | 0            | 0          |
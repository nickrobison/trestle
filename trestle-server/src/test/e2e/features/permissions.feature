# Created by nickrobison at 2/21/18
Feature: User permissions feature
  Validates that users can only see the parts of the application they're authorized to

  @DBA
  Scenario: DBA permissions
    Given I am viewing the dashboard
    When I click the "login" button
    Then Login page appears
    When I login and submit with "dba" and "dba"
    Then I can see 4 "admin" options
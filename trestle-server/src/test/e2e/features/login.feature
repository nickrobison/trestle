# Created by nrobison at 5/31/17
Feature: Login feature
  Tests various login usernames

  Scenario: Try to login with default user
    Given I am viewing the dashboard
    When I click the "login" button
    Then Login page appears

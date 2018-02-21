# Created by nrobison at 5/31/17
Feature: Login page feature

  Scenario: Try login page loading
    Given I am viewing the dashboard
    When I click the "login" button
    Then Login page appears

  Scenario Outline: Check form validation
    Given I am viewing the "login" page
    And I login with <username> and <password>
    Then The login form is validated <valid>

    Examples:
      | username  | password      | valid |
      | validuser |               | false |
      |           | validpassword | false |
      | validuser | validpassword | true  |

  Scenario: Check valid user login
    Given I am viewing the dashboard
    When I click the "login" button
    Then Login page appears
    When I login and submit with "dba" and "wrongPassword"
    Then The error message should be "Incorrect Username or Password"
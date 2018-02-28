# Created by nrobison at 5/31/17
@Login
Feature: Login page feature

  Scenario: Try login page loading
    When I click the "login" button
    Then The "login" page appears


  Scenario Outline: Check form validation
#    We need to reset to the dashboard, because I can't figure out how to reset the form
    Given I am viewing the dashboard
    When I click the "login" button
    And I login with <username> and <password>
    Then The login form is validated <valid>

    Examples:
      | username  | password      | valid |
      | validuser |               | false |
      |           | validpassword | false |
      | validuser | validpassword | true  |

  Scenario: Check valid user login
    Given I click the "login" button
    Then The "login" page appears
    When I login and submit with "dba" and "wrongPassword"
    Then The error message should be "Incorrect Username or Password"

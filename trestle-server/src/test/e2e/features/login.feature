# Created by nrobison at 5/31/17
Feature: Login feature

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



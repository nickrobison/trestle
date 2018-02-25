# Created by nickrobison at 2/23/18
Feature: User Admin Feature
  This feature tests simple user creation/deletion/modification
  Nothing fancy, but useful

  @User
  Scenario: Create User
    Given I click the "login" button
    And I login and submit with "dba" and "dba"
    When I am viewing the "admin/users" page
    Then I create a new "user" with the following properties:
      | username | password        | first_name | last_name | email        |
      | newUser1 | newUserPassword | New        | User      | new@test.com |
#    Then The users table should have 4 users
    When I delete user "newUser1"
#    Then The users table should have 3 users

# Created by nickrobison at 2/23/18
@Users
Feature: User Admin Feature
  This feature tests simple user creation/deletion/modification
  Nothing fancy, but useful

  Scenario: Create User
    Given I login and submit with "dba" and "dba"
    When I am viewing the "admin/users" page
    Then I create a new "user" with the following properties:
      | username | password        | first_name | last_name | email        |
      | newUser1 | newUserPassword | New        | User      | new@test.com |
    Then The users table should have 4 users
    When I delete user "newUser1"
    Then The users table should have 3 users


#  Scenario: Change user password
#    Given I login and submit with "dba" and "dba"
#    When I am viewing the "admin/users" page
#    Then I edit user "user" properties:
#      | password        |
#      | newUserPassword |
#    Then I logout
#    When I click the "login" button
#    And I login and submit with "user" and "newUserPassword"
#    Then The "explore/viewer" page appears
##    Reset the state
#    Given I login and submit with "admin" and "admin1"
#    When I am viewing the "admin/users" page
#    Then I edit user "user" properties:
#      | password |
#      | user1    |
#    Then I logout
#    And I login and submit with "user" and "newUserPassword"
#    Then The error message should be "Incorrect Username or Password"




Feature: Assertion capture

  Scenario: Passing assertions are listed under their step
    Given a login response with status 200 for "alice"
    Then the status should be 200
    And the user name should be "alice"

  Scenario: A failing assertion is listed with its message
    Given a login response with status 404 for "alice"
    Then the status should be 200

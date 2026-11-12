Feature: Selenium capture in Cucumber

  Scenario: Browser actions appear under their steps
    When I open "https://example.test/about"
    And I log in
    Then I click the missing button

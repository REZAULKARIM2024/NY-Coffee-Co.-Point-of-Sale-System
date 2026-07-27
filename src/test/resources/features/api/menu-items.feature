Feature: Menu items API

  Background:
    Given the API server is running

  Scenario: Listing active menu items returns an array
    When I GET "/api/menu-items"
    Then the response status should be 200
    And the response field "items" should be a list

  Scenario: Requesting a non-existent menu item returns 404
    When I GET "/api/menu-items/999999"
    Then the response status should be 404

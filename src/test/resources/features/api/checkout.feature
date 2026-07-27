Feature: Checkout API

  Background:
    Given the API server is running

  Scenario: Checkout without a userId is rejected
    When I POST "/api/checkout" with body:
      """
      {"paymentMethod":"CASH","items":[{"menuItemId":1,"quantity":1}]}
      """
    Then the response status should be 400

  Scenario: Checking out the first available menu item succeeds and the order can be fetched
    Given I look up the first active menu item
    When I checkout that menu item as user 1
    Then the response status should be 201
    And I can fetch the resulting order and it contains at least 1 item

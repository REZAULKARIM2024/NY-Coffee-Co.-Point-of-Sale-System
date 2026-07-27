Feature: API health check
  As a client of the NY Coffee Co. REST API
  I want to verify the service and its database connection are healthy

  Scenario: Health endpoint reports ok status
    Given the API server is running
    When I GET "/api/health"
    Then the response status should be 200
    And the response field "status" should equal "ok"

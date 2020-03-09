Meta:

Narrative:
As a user
I want to fetch the latest Engine Config for the bot
So that I can check that the Engine config is set as expected

Scenario: a user attempts to fetch the bot's Engine config with valid token

Given a valid Engine Config API path
When I call the API with valid credentials
Then the bot will respond with expected Engine config

Scenario: a user attempts to fetch the bot's Engine config with missing token

Given a valid Engine Config API path
When I call the API without credentials
Then the bot will respond with 401 Unauthorized

Scenario: a user attempts to fetch the bot's Engine config using invalid URL

Given an invalid Engine Config API path
When I call the API with valid credentials
Then the bot will respond with 404 Not Found

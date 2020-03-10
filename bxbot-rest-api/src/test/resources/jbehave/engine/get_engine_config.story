Meta:

Narrative:
As a user
I want to fetch the latest Engine Config for the bot
So that I can verify the bot is configured correctly

Scenario: a user fetches the bot's Engine config with valid token

Given a valid Engine Config API path
When a user calls the API with valid token
Then the bot will respond with expected Engine config

Scenario: a user fails to fetch the bot's Engine config with missing token

Given a valid Engine Config API path
When a user calls the API without valid token
Then the bot will respond with 401 Unauthorized

Scenario: a user fails to fetch the bot's Engine config using invalid URL

Given an invalid Engine Config API path
When a user calls the API with valid token
Then the bot will respond with 404 Not Found

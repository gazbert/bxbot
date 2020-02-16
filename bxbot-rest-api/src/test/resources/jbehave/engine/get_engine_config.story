Meta:

Narrative:
As a user
I want to fetch the latest Engine Config for the bot
So that I can check that the Engine config is set as expected

Scenario: a user attempts to fetch the bot's Engine config

Given a valid Engine Config API path
When I call the API without credentials
Then the bot will respond with: 401 Unauthorized

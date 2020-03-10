Meta:

Narrative:
As a administrator
I want to update the Engine Config for the bot
So that I can run the bot with new config settings

Scenario: an administrator updates the bot's Engine config with valid token

Given a valid Engine Config API path
When administrator has a valid token
And administrator calls API with botname UpdatedBotname and tradeCycleInterval of 100
Then the bot will respond with updated Engine config with botname UpdatedBotname and trade cycle interval 100

Scenario: an administrator cannot update the bot's Engine config with missing token

Given a valid Engine Config API path
When administrator does not have valid token
And administrator calls API with botname UpdatedBotname and tradeCycleInterval of 100
Then the bot will respond with 401 Unauthorized

Scenario: an administrator fails to update the bot's Engine config using invalid URL

Given an invalid Engine Config API path
When administrator has a valid token
And administrator calls API with botname UpdatedBotname and tradeCycleInterval of 100
Then the bot will respond with 404 Not Found

Scenario: a user cannot update the bot's Engine config

Given a valid Engine Config API path
When user calls API to update Engine Config with botname UpdatedBotname and tradeCycleInterval of 100
Then the bot will respond with 403 Forbidden

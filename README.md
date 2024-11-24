![Streamlabs Integration](https://github.com/Membercat-Studios/StreamLabsPlugin/blob/main/minelabs_icon_text.png?raw=true)
# Streamlabs Integration
A simple minecraft plugin that allows your YouTube and Twitch chat to interact with your Minecraft game via donations.
Visit the [Wiki](https://github.com/Membercat-Studios/StreamLabsPlugin/wiki) for instruction on how to use the configuration!

## Installation
1. Download the latest release from the Releases page on GitHub
2. Put the plugin it the plugins folder
3. Restart the server
4. Go to The Streamlabs dashboard. Then, in the top right corner, click on the account icon and go to *Account Settings*
5. Click on *API Settings* and copy your socket token
6. Go to /plugins/Streamlabs/config.yml and paste the socket token in the config
7. Enter `/streamlabs reload` and wait for the plugin to connect to the streamlabs API

## Commands
- `/streamlabs reload`: Loads changes in the config and reconnects
- `/streamlabs status`: Shows whether the plugin is currently connected to the Streamlabs API
- `/streamlabs connect`: Used to reconnect to Streamlabs after getting disconnected
- `/streamlabs disconnect`: Disconnects from the Streamlabs API
- `/streamlabs player add {name}`: Adds a player to the `affected_players` config
- `/streamlabs player remove {name}`: Removes a player from the `affected_players` config

## Configuration
To get started, put your Streamlabs socket token in the `socket_token` field:
```yaml
streamlabs:
  socket_token: "(Your Streamlabs Socket token here)"
```

The plugin works based on **actions** that tell the plugin what to do in a specific scenario.
You can add as many actions as you like in the `actions` field. An action has the following fields:
```yaml
actions:
  example_action:
    enabled: true # Whether the action is enabled
    action: (Action type)
    conditions:
      - (List of conditions that have to be met in order for the action to execute)
    donation_condition:
      - (All conditions with the currency of the received donation will be checked, this will be ignored if the event is not a donation)
    messages:
      - (List of messages that will be sent in chat or as a title)
    commands:
      - (List of minecraft commands to execute)
```

## Default configuration
```yaml
streamlabs:
  socket_token: "" # Put your Streamlabs socket token here

affected_players: # Players that will be affected by the actions {player}
  - domplanto
  - codingcat

show_status_messages: true # Whether the plugin will send status messages in chat (for example "Successfully connected to Streamlabs")

actions:
  example_reward:
    enabled: true # Whether the action is enabled
    action: streamlabs_donation # The action that will trigger it
    conditions: # Conditions that must be met for the action to trigger
      - '{message}.>cats are cool'
      - '{user}=codingcat24'
    donation_conditions: # All conditions with the currency of the received donation will be checked (this will not be checked if the event is not a donation)
      - "EUR>10"
      - "EUR<50"
      - "USD>10.54"
      - "USD<50"
      - "AUD>16.31"
      - "AUD<50"
    messages: # Messages that will be sent when the action triggers. USE § FOR COLOR CODES, NOT &!
      - '[message]§l§6{user} §r§9donated {amount_formatted}!'
      - '[title]§cNew Donation!'
      - '[subtitle]§a{user} §9donated {amount_formatted}!'
    commands: # Commands that will be executed when the action triggers. for ' do ''.
      - 'give {player} diamond {amount}'
      - 'effect give {player} regeneration {amount} 1'
      - '[{amount}/10]execute at {player} run summon zombie ~ ~ ~ {CustomName:''[{"text":"{user}"}]''}' # [{amount}/10] will be replaced with the amount divided by 10
```


[<img src="https://codingcat2468.github.io/assets/images/membercat_studios.png" height="55" width="200"/>](https://membercat.com)

# Streamlabs Plugin

A simple plugin that allows your YouTube and Twitch chat to interact with your Minecraft game .

## Installation

1. Download the latest release from the Releases page on Github
2. Put the plugin it the plugins folder
3. Restart the server
4. Go to The Streamlabs dashboard. Than in the top right go to you account and go to Account Settings
5. Than go to api settings than api tokens and copy the Socket API Token
6. Go to /plugins/Streamlabs/config.yml and paste the Socket API Token in de config
7. than do /streamlabs reload

## Usage

1. to connect use /streamlabs connect
2. to disconnect use /streamlabs disconnect
3. to see the status use /streamlabs status
4. to reload the config use /streamlabs reload

## Configuration
```
actions:
  (name):
    enabled: true
    action: (Actions)
    threshold: 5.0
    commands:
      - "give {player} diamond {amount}"
      - "effect give {player} regeneration 30 1"
```

## Actions

 - `streamlabs_donation`: When someone donates on streamlabs
 - `twitch_follow`: When someone follows on Twitch
 - `twitch_bits`: When someone donates Bits on Twitch
 - `twitch_subscription`: When someone subscribed on Twitch
 - `twitch_raid`: When someone raides your stream on Twitch
 - `twitch_host`: 
 - `youtube_subscription`: 
 - `youtube_superchat`: 
 - `youtube_membership`: 
 - `youtube_gift_memberships`: 

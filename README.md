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

## Commands

- to connect use /streamlabs connect
- to disconnect use /streamlabs disconnect
- to see the status use /streamlabs status
- to reload the config use /streamlabs reload
- to add affected players use /streamlabs player add (Username)
- to remove affected players use /streamlabs player remove (Username)

## Configuration
To create a action you put in in the config.yml in this format
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
To add affected players edit this in the config.yml
```
affected_players:
  - "Domplanto"
  - "codingcat"
  - "(Minecraft Username)"
```
To make the plugin connect to Streamlabs add your Socket API token here. How to get the API token look hire [Installation](#Instalation)
```
streamlabs:
  socket_token: "(Your Streamlabs Socket token here)"
```

## Actions

 - `streamlabs_donation`: When someone donates on streamlabs
 - `twitch_follow`: When someone follows on Twitch
 - `twitch_bits`: When someone donates Bits on Twitch
 - `twitch_subscription`: When someone subscribed on Twitch
 - `twitch_raid`: When someone raides your stream on Twitch
 - `youtube_subscription`: When someone subscribes on YouTube
 - `youtube_superchat`: When someone sends a superchat on YouTube
 - `youtube_membership`: When someone buys a membership on YouTube
 - `youtube_gift_memberships`: When someone giftes a membership on YouTube

 - `twitch_host`: No documentation found

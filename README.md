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
```bash
actions:
  (name):
    enabled: true
    action: (action)
    threshold: 5.0
    commands:
      - "give {player} diamond {amount}"
      - "effect give {player} regeneration 30 1"
```

### Basic Settings
- `Alert Duration`: How long alerts appear (in seconds)
- `Animation Style`: Choose between Fade, Bounce, or Slide
- `Sound Volume`: Adjust the volume of alert sounds (0-100)

### Advanced Settings
- `Custom CSS`: Add your own styles
- `Webhook URL`: For custom integrations
- `Debug Mode`: Enable for troubleshooting

## Development

### Prerequisites
- Node.js 14+
- npm or yarn
- Streamlabs Desktop

### Setup
```bash
# Clone the repository
git clone https://github.com/yourusername/streamlabs-plugin

# Install dependencies
npm install

# Build the plugin
npm run build
```

### Testing
```bash
npm run test
```

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Support

For support, please:
- Check our [FAQ](link-to-faq)
- Join our [Discord community](link-to-discord)
- Open an issue on GitHub

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

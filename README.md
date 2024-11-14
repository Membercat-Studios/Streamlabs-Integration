# Streamlabs Plugin

A simple plugin that allows your YouTube and Twitch chat to interact with your Minecraft game .

## Features

- Custom alerts integration

## Installation

1. Download the latest release from the Releases page
2. Open Streamlabs Desktop
3. Navigate to Settings → Advanced
4. Click on "Install Plugin"
5. Select the downloaded .streamlabsplugin file
6. Restart Streamlabs Desktop

## Usage

1. After installation, go to your Streamlabs Dashboard
2. Find the plugin under the "Installed Plugins" section
3. Configure your desired settings
4. Add the plugin widget to your scene using the + button

## Configuration

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

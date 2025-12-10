# Spotify Music Recommender

## Setup
1. Get API Key From https://developer.spotify.com/ by creating an app
2. Scroll down and make sure your Redirect URIs is set to http://127.0.0.1:8888/callback (You could switch that 8888 to anything you like, but make sure to change it in the config files as well)
2. Create a config folder under src
3. Create a Config.java in src/config
4. Run Main.java, but make sure to have JavaFX library and VM Options configured if using intellij

## Features
- 2 Different Recommendation Mode
  - Popularity dependent
  - Artist Dependent
- Search before asking for recommendation

## Design Patterns
- Strategy: Different Recommendation Strategy
- Factory: Request Creation
- Observer: UI Updates

## Demo: https://drive.google.com/file/d/1qrxoIzFqGRrS1RUbY0Sr0cbwzABAsBnn/view?usp=sharing
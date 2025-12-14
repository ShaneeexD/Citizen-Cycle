# Citizen Cycle

A RuneLite plugin for coordinating Wealthy Citizen thieving across worlds. Similar to Star Miners, this plugin crowdsources distraction data so you can efficiently hop between worlds where citizens are distracted.

## Features

- **Distraction Detection**: Automatically detects when a Wealthy Citizen is distracted by a child NPC
- **Crowdsourcing**: Shares distraction status with other plugin users via configurable API endpoint
- **World Hopping**: Quick-hop to worlds with distracted citizens directly from the side panel
- **Visual Overlay**: Highlights distracted citizens and shows distraction timer
- **Side Panel**: Displays all worlds with their citizen status, sorted by distraction state
- **Notifications**: Audio alerts when distraction starts/ends

## Configuration

### General
- **Show Side Panel**: Toggle the side panel visibility

### Crowdsourcing
- **API Endpoint**: URL for the crowdsourcing server
- **Authorization**: Auth token for the API
- **Auto Broadcast**: Automatically share distraction status
- **Refresh Interval**: How often to fetch data from server (seconds)

### Overlay
- **Show Overlay**: Toggle the in-game overlay
- **Highlight Distracted Citizen**: Highlight NPCs when distracted
- **Distracted Color**: Color for distracted citizen highlight
- **Not Distracted Color**: Color for non-distracted citizens
- **Show Distraction Timer**: Display duration of distraction

### Notifications
- **Notify on Distraction**: Sound when distraction starts
- **Notify on Distraction End**: Sound when distraction ends
- **Notify World Available**: Alert when another world has a distracted citizen

## How It Works

1. When you're near Wealthy Citizens in the thieving area, the plugin monitors for distractions
2. A citizen is considered "distracted" when interacting with a child NPC (combat level 0)
3. The plugin broadcasts this status to the configured API endpoint
4. Other users receive updates showing which worlds have distracted citizens
5. Click "Hop" on any world in the side panel to quick-hop there

## API Format

The plugin expects the API to return JSON in this format:
```json
[
  {
    "world": 301,
    "distracted": true,
    "distractionStartTime": 1702567890000,
    "lastUpdateTime": 1702567895000,
    "reportedBy": "PlayerName"
  }
]
```

## Building

```bash
./gradlew build
```

## Credits

Inspired by:
- [Star Calling Assist](https://github.com/zodaz/star-calling-assist) - World coordination pattern
- [Idle Master](https://github.com/ShaneeexD/idle-master) - Wealthy Citizen detection logic
Exact Path Preview Plugin for RuneLite - by dinked it (blindly/blindtcg)

A RuneLite plugin that displays the exact path your character will take when hovering over a tile, using breadth-first search, a pathfinding algorithm utilized by OSRS.

Features

Core Functionality

100% Accurate Pathfinding - Uses OSRS's Breadth-First Search algorithm with exact tile checking order

Real-time Path Updates - Updates every game tick based on hovered tile

Checkpoint Mode - Option to show only the 25 checkpoint tiles (corners) that OSRS uses internally

Fallback Path Support - Shows path to nearest reachable tile when exact destination is blocked

Collision Detection - Full support for walls, objects, diagonal blocking, and directional movement

Customization Options

General Settings

Enable/disable plugin on the fly
Show checkpoints only (matches OSRS's 25 checkpoint limit)
Toggle fallback path display
Debug mode with pathfinding logs

Path Appearance

Custom fill colors with alpha transparency
Fade effect (gradually fade from start to destination)
Configurable fade opacity (start and end alpha)
Toggle tile filling on/off

Border Settings

Adjustable border width (1-8 pixels)
Custom border colors
Three border styles: Solid, Dashed, Dotted
Toggle borders on/off

Destination Tile

Separate highlight color for destination
Custom destination border color and width
Optional pulsing animation effect
Toggle destination highlighting

Start Tile

Optional start tile highlighting
Custom start tile color
Adjustable border width

Path Information Overlay

Display tile count
Show distance to destination
Show direct vs actual path comparison
Performance metrics (calculation time)
Fallback path indicator

Performance Options

Anti-aliasing toggle
Maximum path display length (10-500 tiles)
Optimized rendering

Installation
From Plugin Hub

Open RuneLite
Click the wrench icon (Configuration)
Click "Plugin Hub" at the bottom
Search for "Exact Path Preview"
Click "Install"

Manual Installation

Clone this repository
Place the source files in your RuneLite plugins directory:

   runelite/plugins/src/main/java/com/exactpathpreview/

Build RuneLite from source

File Structure
com/exactpathpreview/
├── ExactPathPreviewPlugin.java        # Main plugin class
├── ExactPathPreviewConfig.java        # Configuration interface
├── ExactPathPreviewOverlay.java       # Path rendering overlay
└── ExactPathPreviewInfoOverlay.java   # Information panel overlay

How It Works

OSRS Pathfinding Algorithm

This plugin implements the exact pathfinding algorithm used by Old School RuneScape:

Breadth-First Search (BFS) - Not A* or Dijkstra
Specific Tile Order - Checks neighbors in OSRS order: W, E, S, N, SW, SE, NW, NE
128x128 Grid Constraint - Paths never exceed this area
Reachability Check - Destination must be within 101x101 tiles
Collision Detection - Respects walls, objects, and diagonal blocking
Checkpoint Extraction - Finds corners/direction changes (max 25)
Fallback Search - 21x21 grid search when exact tile unreachable

Performance

Typical pathfinding calculation: 100-500 microseconds
Uses efficient BFS with early termination
Minimal impact on game performance
Optimized collision data caching

Configuration Examples

Minimal Setup (Clean Look)
- Fill Tiles: ON
- Path Color: Cyan with 60 alpha
- Draw Borders: OFF
- Highlight Destination: ON
- Show Path Info: OFF

Maximum Visibility
- Fill Tiles: ON
- Path Fade Effect: ON
- Border Width: 3
- Border Style: SOLID
- Pulse Destination: ON
- Show Path Info: ON

Performance Mode
- Anti-Aliasing: OFF
- Max Path Length: 100
- Show Checkpoints Only: ON
- Fill Tiles: OFF (borders only)

Debug Mode
- Debug Mode: ON
- Show Calculation Time: ON
- Show Path Info: ON
- Show Distance: ON

Technical Details

Collision Detection

The plugin reads collision data directly from the client's collision maps and checks:

Full tile blocking (impassable objects)
Directional blocking (walls on tile edges)
Diagonal blocking (pillars on tile corners)
Adjacent tile blocking for diagonal movement

Path Calculation
Uses a Queue-based BFS implementation with:

HashMap for visited tiles tracking
Distance tracking from start
Parent pointers for path reconstruction
Western/southern priority for fallback search

Rendering

Uses RuneLite's Perspective API for 3D to 2D conversion
Polygon-based tile rendering
Alpha blending for transparency
Optional fade and pulse effects
Multiple stroke styles for borders

Known Limitations

Plane Changes - Does not show paths across different game planes (e.g., different floors)
Dynamic Objects - Doesn't predict doors opening/closing or temporary obstacles
NPCs - Doesn't account for NPC blocking (matches OSRS behavior)
Very Long Paths - Paths exceeding max display length are truncated for performance

Troubleshooting

Path Not Showing

Ensure plugin is enabled in config
Check if destination is on same plane
Verify destination is within 101x101 tile range
Try enabling "Show Fallback Path"

Performance Issues

Reduce "Max Path Display Length"
Disable "Anti-Aliasing"
Enable "Show Checkpoints Only"
Turn off "Path Fade Effect"

Inaccurate Paths

This should not occur - please report as a bug
Enable debug mode and check logs
Verify you're not clicking on objects (only tiles)

Contributing
Contributions are welcome! Please:

Fork the repository
Create a feature branch
Test thoroughly
Submit a pull request

Credits

OSRS Wiki - Pathfinding algorithm documentation
Runemoro - Shortest Path plugin reference
RuneLite Team - API and framework

License
This plugin is licensed under the BSD 2-Clause License.
Support
For issues, suggestions, or questions:

Create an issue on GitHub
Join the RuneLite Discord
Check the Plugin Hub discussions

Version History
v1.0.0 (Initial Release)

Full OSRS pathfinding implementation
Comprehensive configuration options
Path information overlay
Multiple rendering styles
Performance optimizations

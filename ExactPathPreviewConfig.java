package net.runelite.client.plugins.exactpathpreview;

import net.runelite.client.config.*;

import java.awt.Color;

@ConfigGroup("exactpathpreview")
public interface ExactPathPreviewConfig extends Config {

    // ==================== GENERAL SETTINGS ====================

    @ConfigSection(
            name = "General Settings",
            description = "General plugin settings",
            position = 0
    )
    String generalSection = "general";

    @ConfigItem(
            keyName = "enabled",
            name = "Enable Plugin",
            description = "Enable or disable the path preview",
            position = 0,
            section = generalSection
    )
    default boolean enabled() {
        return true;
    }

    @ConfigItem(
            keyName = "highPerformanceMode",
            name = "High Performance Mode",
            description = "Updates path every frame (~50 FPS) instead of every game tick (~1.6 FPS). Essential for fast-paced PvM!",
            position = 1,
            section = generalSection,
            warning = "This is already enabled by default for PvM. Disable only if you experience lag."
    )
    default boolean highPerformanceMode() {
        return true;
    }

    @ConfigItem(
            keyName = "showCheckpointsOnly",
            name = "Show Checkpoints Only",
            description = "Show only checkpoint tiles (corners) instead of full path. This matches OSRS's 25 checkpoint limit.",
            position = 2,
            section = generalSection
    )
    default boolean showCheckpointsOnly() {
        return false;
    }

    @ConfigItem(
            keyName = "showFallbackPath",
            name = "Show Fallback Path",
            description = "When exact tile cannot be reached, show path to nearest reachable tile",
            position = 3,
            section = generalSection
    )
    default boolean showFallbackPath() {
        return true;
    }

    @ConfigItem(
            keyName = "debugMode",
            name = "Debug Mode",
            description = "Enable debug logging for pathfinding calculations",
            position = 4,
            section = generalSection
    )
    default boolean debugMode() {
        return false;
    }

    // ==================== PATH APPEARANCE ====================

    @ConfigSection(
            name = "Path Appearance",
            description = "Customize the appearance of the path tiles",
            position = 1,
            closedByDefault = false
    )
    String pathSection = "path";

    @ConfigItem(
            keyName = "fillTiles",
            name = "Fill Path Tiles",
            description = "Fill the path tiles with color",
            position = 0,
            section = pathSection
    )
    default boolean fillTiles() {
        return true;
    }

    @ConfigItem(
            keyName = "pathColor",
            name = "Path Fill Color",
            description = "The color used to fill path tiles",
            position = 1,
            section = pathSection
    )
    @Alpha
    default Color pathColor() {
        return new Color(0, 200, 255, 80);
    }

    @ConfigItem(
            keyName = "pathFadeEffect",
            name = "Path Fade Effect",
            description = "Gradually fade path tiles from start to destination",
            position = 2,
            section = pathSection
    )
    default boolean pathFadeEffect() {
        return false;
    }

    @ConfigItem(
            keyName = "fadeStartAlpha",
            name = "Fade Start Alpha",
            description = "Starting opacity for fade effect (0-255)",
            position = 3,
            section = pathSection
    )
    @Range(min = 0, max = 255)
    default int fadeStartAlpha() {
        return 40;
    }

    @ConfigItem(
            keyName = "fadeEndAlpha",
            name = "Fade End Alpha",
            description = "Ending opacity for fade effect (0-255)",
            position = 4,
            section = pathSection
    )
    @Range(min = 0, max = 255)
    default int fadeEndAlpha() {
        return 150;
    }

    // ==================== BORDER SETTINGS ====================

    @ConfigSection(
            name = "Border Settings",
            description = "Customize tile borders",
            position = 2,
            closedByDefault = true
    )
    String borderSection = "border";

    @ConfigItem(
            keyName = "drawBorder",
            name = "Draw Tile Borders",
            description = "Draw borders around path tiles",
            position = 0,
            section = borderSection
    )
    default boolean drawBorder() {
        return true;
    }

    @ConfigItem(
            keyName = "borderWidth",
            name = "Border Width",
            description = "Width of the tile borders in pixels",
            position = 1,
            section = borderSection
    )
    @Range(min = 1, max = 8)
    default int borderWidth() {
        return 2;
    }

    @ConfigItem(
            keyName = "borderColor",
            name = "Border Color",
            description = "Color of the tile borders",
            position = 2,
            section = borderSection
    )
    @Alpha
    default Color borderColor() {
        return new Color(0, 220, 255, 200);
    }

    @ConfigItem(
            keyName = "borderStyle",
            name = "Border Style",
            description = "Style of the border lines",
            position = 3,
            section = borderSection
    )
    default BorderStyle borderStyle() {
        return BorderStyle.SOLID;
    }

    // ==================== DESTINATION HIGHLIGHT ====================

    @ConfigSection(
            name = "Destination Tile",
            description = "Customize the destination tile appearance",
            position = 3,
            closedByDefault = true
    )
    String destinationSection = "destination";

    @ConfigItem(
            keyName = "highlightDestination",
            name = "Highlight Destination",
            description = "Highlight the destination tile with a different color",
            position = 0,
            section = destinationSection
    )
    default boolean highlightDestination() {
        return true;
    }

    @ConfigItem(
            keyName = "destinationColor",
            name = "Destination Fill Color",
            description = "Color used to fill the destination tile",
            position = 1,
            section = destinationSection
    )
    @Alpha
    default Color destinationColor() {
        return new Color(255, 50, 50, 120);
    }

    @ConfigItem(
            keyName = "destinationBorderColor",
            name = "Destination Border Color",
            description = "Color of the destination tile border",
            position = 2,
            section = destinationSection
    )
    @Alpha
    default Color destinationBorderColor() {
        return new Color(255, 80, 80, 255);
    }

    @ConfigItem(
            keyName = "destinationBorderWidth",
            name = "Destination Border Width",
            description = "Width of the destination tile border",
            position = 3,
            section = destinationSection
    )
    @Range(min = 1, max = 10)
    default int destinationBorderWidth() {
        return 3;
    }

    @ConfigItem(
            keyName = "pulseDestination",
            name = "Pulse Destination",
            description = "Add a pulsing animation to the destination tile",
            position = 4,
            section = destinationSection
    )
    default boolean pulseDestination() {
        return false;
    }

    // ==================== START TILE ====================

    @ConfigSection(
            name = "Start Tile",
            description = "Customize the start tile (player position) appearance",
            position = 4,
            closedByDefault = true
    )
    String startSection = "start";

    @ConfigItem(
            keyName = "highlightStart",
            name = "Highlight Start Tile",
            description = "Highlight the starting tile (player position)",
            position = 0,
            section = startSection
    )
    default boolean highlightStart() {
        return false;
    }

    @ConfigItem(
            keyName = "startColor",
            name = "Start Tile Color",
            description = "Color used to highlight the start tile",
            position = 1,
            section = startSection
    )
    @Alpha
    default Color startColor() {
        return new Color(50, 255, 50, 100);
    }

    @ConfigItem(
            keyName = "startBorderWidth",
            name = "Start Border Width",
            description = "Width of the start tile border",
            position = 2,
            section = startSection
    )
    @Range(min = 1, max = 10)
    default int startBorderWidth() {
        return 2;
    }

    // ==================== PATH INFO OVERLAY ====================

    @ConfigSection(
            name = "Path Information",
            description = "Display path statistics on screen",
            position = 5,
            closedByDefault = true
    )
    String infoSection = "info";

    @ConfigItem(
            keyName = "showPathInfo",
            name = "Show Path Info",
            description = "Display path information (distance, tiles) on screen",
            position = 0,
            section = infoSection
    )
    default boolean showPathInfo() {
        return false;
    }

    @ConfigItem(
            keyName = "showTileCount",
            name = "Show Tile Count",
            description = "Display the number of tiles in the path",
            position = 1,
            section = infoSection
    )
    default boolean showTileCount() {
        return true;
    }

    @ConfigItem(
            keyName = "showDistance",
            name = "Show Distance",
            description = "Display the distance to the destination",
            position = 2,
            section = infoSection
    )
    default boolean showDistance() {
        return true;
    }

    @ConfigItem(
            keyName = "showCalculationTime",
            name = "Show Calculation Time",
            description = "Display how long the pathfinding took (debug)",
            position = 3,
            section = infoSection
    )
    default boolean showCalculationTime() {
        return false;
    }

    @ConfigItem(
            keyName = "infoTextColor",
            name = "Info Text Color",
            description = "Color of the path information text",
            position = 4,
            section = infoSection
    )
    @Alpha
    default Color infoTextColor() {
        return Color.WHITE;
    }

    // ==================== PERFORMANCE ====================

    @ConfigSection(
            name = "Performance",
            description = "Performance and rendering options",
            position = 6,
            closedByDefault = true
    )
    String performanceSection = "performance";

    @ConfigItem(
            keyName = "antiAliasing",
            name = "Anti-Aliasing",
            description = "Enable anti-aliasing for smoother borders (may impact performance)",
            position = 0,
            section = performanceSection
    )
    default boolean antiAliasing() {
        return true;
    }

    @ConfigItem(
            keyName = "maxPathLength",
            name = "Max Path Display Length",
            description = "Maximum number of tiles to display in path (helps with performance for very long paths)",
            position = 1,
            section = performanceSection
    )
    @Range(min = 10, max = 500)
    default int maxPathLength() {
        return 200;
    }

    // ==================== TRAIL SETTINGS ====================

    @ConfigSection(
            name = "Trail Effect",
            description = "Show sparkly effect on tiles you walked on",
            position = 7,
            closedByDefault = true
    )
    String trailSection = "trail";

    @ConfigItem(
            keyName = "showTrail",
            name = "Enable Trail",
            description = "Show sparkle effect on recently walked tiles",
            position = 0,
            section = trailSection
    )
    default boolean showTrail() {
        return false;
    }

    @ConfigItem(
            keyName = "trailLength",
            name = "Trail Length",
            description = "Number of tiles to keep in trail",
            position = 1,
            section = trailSection
    )
    @Range(min = 1, max = 50)
    default int trailLength() {
        return 10;
    }

    @ConfigItem(
            keyName = "trailDuration",
            name = "Trail Duration (ms)",
            description = "How long trail tiles remain visible",
            position = 2,
            section = trailSection
    )
    @Range(min = 500, max = 10000)
    default int trailDuration() {
        return 3000;
    }

    @ConfigItem(
            keyName = "trailColor",
            name = "Trail Color",
            description = "Color of the trail sparkles",
            position = 3,
            section = trailSection
    )
    @Alpha
    default Color trailColor() {
        return new Color(255, 215, 0, 180);
    }

    @ConfigItem(
            keyName = "trailSparkleSize",
            name = "Sparkle Size",
            description = "Size of sparkle particles",
            position = 4,
            section = trailSection
    )
    @Range(min = 1, max = 10)
    default int trailSparkleSize() {
        return 3;
    }

    @ConfigItem(
            keyName = "trailFadeOut",
            name = "Fade Out Trail",
            description = "Gradually fade trail over time",
            position = 5,
            section = trailSection
    )
    default boolean trailFadeOut() {
        return true;
    }

    @ConfigItem(
            keyName = "trailFillTiles",
            name = "Fill Trail Tiles",
            description = "Fill trail tiles with transparent color",
            position = 6,
            section = trailSection
    )
    default boolean trailFillTiles() {
        return true;
    }

    @ConfigItem(
            keyName = "trailFillColor",
            name = "Trail Fill Color",
            description = "Color to fill trail tiles",
            position = 7,
            section = trailSection
    )
    @Alpha
    default Color trailFillColor() {
        return new Color(255, 215, 0, 50);
    }

    @ConfigItem(
            keyName = "trailBorder",
            name = "Trail Border",
            description = "Draw border around trail tiles",
            position = 8,
            section = trailSection
    )
    default boolean trailBorder() {
        return true;
    }

    @ConfigItem(
            keyName = "trailBorderWidth",
            name = "Trail Border Width",
            description = "Width of trail tile borders",
            position = 9,
            section = trailSection
    )
    @Range(min = 1, max = 5)
    default int trailBorderWidth() {
        return 2;
    }

    @ConfigItem(
            keyName = "trailBorderColor",
            name = "Trail Border Color",
            description = "Color of trail tile borders",
            position = 10,
            section = trailSection
    )
    @Alpha
    default Color trailBorderColor() {
        return new Color(255, 215, 0, 150);
    }

    // Enum for border style
    enum BorderStyle {
        SOLID,
        DASHED,
        DOTTED
    }
}
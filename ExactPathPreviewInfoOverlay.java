package net.runelite.client.plugins.exactpathpreview;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;

public class ExactPathPreviewInfoOverlay extends OverlayPanel {

    private final Client client;
    private final ExactPathPreviewPlugin plugin;
    private final ExactPathPreviewConfig config;

    @Inject
    public ExactPathPreviewInfoOverlay(Client client, ExactPathPreviewPlugin plugin, ExactPathPreviewConfig config) {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enabled() || !config.showPathInfo()) {
            return null;
        }

        List<WorldPoint> path = plugin.getCurrentPath();
        WorldPoint hoveredTile = plugin.getHoveredTile();
        Player localPlayer = client.getLocalPlayer();

        if (path == null || path.isEmpty() || localPlayer == null || hoveredTile == null) {
            return null;
        }

        panelComponent.getChildren().clear();

        // Title
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Path Preview")
                .color(config.infoTextColor())
                .build());

        // Tile count
        if (config.showTileCount()) {
            String tileCountText = "Tiles: " + path.size();
            if (config.showCheckpointsOnly()) {
                tileCountText += " (checkpoints)";
            }

            panelComponent.getChildren().add(LineComponent.builder()
                    .left(tileCountText)
                    .leftColor(config.infoTextColor())
                    .build());
        }

        // Distance information
        if (config.showDistance()) {
            WorldPoint playerPos = localPlayer.getWorldLocation();
            int dx = Math.abs(hoveredTile.getX() - playerPos.getX());
            int dy = Math.abs(hoveredTile.getY() - playerPos.getY());
            int chebyshevDistance = Math.max(dx, dy);

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Direct: " + chebyshevDistance + " tiles")
                    .leftColor(config.infoTextColor())
                    .build());

            if (path.size() > chebyshevDistance) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Detour: +" + (path.size() - chebyshevDistance) + " tiles")
                        .leftColor(new Color(255, 200, 100))
                        .build());
            }
        }

        // Calculation time (debug)
        if (config.showCalculationTime()) {
            long calcTime = plugin.getLastCalculationTime();
            String timeText = String.format("Calc: %.2f μs", calcTime / 1000.0);

            panelComponent.getChildren().add(LineComponent.builder()
                    .left(timeText)
                    .leftColor(new Color(150, 150, 150))
                    .build());
        }

        // Show if using fallback path
        if (config.showFallbackPath() && !path.isEmpty()) {
            WorldPoint destination = path.get(path.size() - 1);
            if (!destination.equals(hoveredTile)) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Using nearest tile")
                        .leftColor(new Color(255, 150, 0))
                        .build());
            }
        }

        return super.render(graphics);
    }
}
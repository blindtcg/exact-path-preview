package net.runelite.client.plugins.exactpathpreview;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;

public class ExactPathPreviewOverlay extends Overlay {

    private final Client client;
    private final ExactPathPreviewPlugin plugin;
    private final ExactPathPreviewConfig config;

    private long lastPulseTime = 0;
    private static final int PULSE_DURATION = 2000; // 2 seconds for full pulse cycle

    @Inject
    public ExactPathPreviewOverlay(Client client, ExactPathPreviewPlugin plugin, ExactPathPreviewConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.LOW);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enabled()) {
            return null;
        }

        // Configure graphics rendering
        if (config.antiAliasing()) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }

        // Render trail first (behind path)
        if (config.showTrail()) {
            renderTrail(graphics);
        }

        List<WorldPoint> path = plugin.getCurrentPath();
        Player localPlayer = client.getLocalPlayer();

        if (path == null || path.isEmpty() || localPlayer == null) {
            return null;
        }

        // Configure graphics rendering
        if (config.antiAliasing()) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }

        WorldPoint playerLocation = localPlayer.getWorldLocation();
        int pathLength = Math.min(path.size(), config.maxPathLength());

        // Render each tile in the path
        for (int i = 0; i < pathLength; i++) {
            WorldPoint point = path.get(i);

            // Check if on same plane
            if (point.getPlane() != client.getPlane()) {
                continue;
            }

            LocalPoint localPoint = LocalPoint.fromWorld(client, point);
            if (localPoint == null) {
                continue;
            }

            Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
            if (poly == null) {
                continue;
            }

            // Determine what type of tile this is
            boolean isStart = (i == 0 && config.highlightStart());
            boolean isDestination = (i == pathLength - 1 && config.highlightDestination());

            if (isStart) {
                renderStartTile(graphics, poly);
            } else if (isDestination) {
                renderDestinationTile(graphics, poly);
            } else {
                renderPathTile(graphics, poly, i, pathLength);
            }
        }

        return null;
    }

    private void renderPathTile(Graphics2D graphics, Polygon poly, int index, int totalTiles) {
        // Calculate fade effect if enabled
        Color fillColor = config.pathColor();
        Color borderColor = config.borderColor();

        if (config.pathFadeEffect() && totalTiles > 1) {
            float progress = (float) index / (totalTiles - 1);
            int startAlpha = config.fadeStartAlpha();
            int endAlpha = config.fadeEndAlpha();
            int alpha = (int) (startAlpha + (endAlpha - startAlpha) * progress);

            fillColor = new Color(
                    fillColor.getRed(),
                    fillColor.getGreen(),
                    fillColor.getBlue(),
                    Math.min(255, Math.max(0, alpha))
            );
        }

        // Fill tile
        if (config.fillTiles()) {
            graphics.setColor(fillColor);
            graphics.fillPolygon(poly);
        }

        // Draw border
        if (config.drawBorder()) {
            graphics.setColor(borderColor);
            Stroke stroke = createStroke(config.borderWidth(), config.borderStyle());
            graphics.setStroke(stroke);
            graphics.drawPolygon(poly);
        }
    }

    private void renderDestinationTile(Graphics2D graphics, Polygon poly) {
        Color fillColor = config.destinationColor();
        Color borderColor = config.destinationBorderColor();

        // Apply pulse effect if enabled
        if (config.pulseDestination()) {
            long currentTime = System.currentTimeMillis();
            float pulseProgress = (float) ((currentTime % PULSE_DURATION) / (double) PULSE_DURATION);

            // Create sine wave pulse (0.7 to 1.0 scale)
            float pulseScale = 0.7f + 0.3f * (float) Math.sin(pulseProgress * Math.PI * 2);

            int pulsedAlpha = (int) (fillColor.getAlpha() * pulseScale);
            fillColor = new Color(
                    fillColor.getRed(),
                    fillColor.getGreen(),
                    fillColor.getBlue(),
                    Math.min(255, Math.max(0, pulsedAlpha))
            );
        }

        // Fill destination tile
        if (config.fillTiles()) {
            graphics.setColor(fillColor);
            graphics.fillPolygon(poly);
        }

        // Draw destination border
        if (config.drawBorder()) {
            graphics.setColor(borderColor);
            graphics.setStroke(new BasicStroke(config.destinationBorderWidth()));
            graphics.drawPolygon(poly);
        }
    }

    private void renderStartTile(Graphics2D graphics, Polygon poly) {
        Color fillColor = config.startColor();

        // Fill start tile
        graphics.setColor(fillColor);
        graphics.fillPolygon(poly);

        // Draw start border
        graphics.setColor(new Color(
                fillColor.getRed(),
                fillColor.getGreen(),
                fillColor.getBlue(),
                Math.min(255, fillColor.getAlpha() + 100)
        ));
        graphics.setStroke(new BasicStroke(config.startBorderWidth()));
        graphics.drawPolygon(poly);
    }

    private Stroke createStroke(int width, ExactPathPreviewConfig.BorderStyle style) {
        switch (style) {
            case DASHED:
                float[] dashPattern = {10.0f, 10.0f};
                return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f);
            case DOTTED:
                float[] dotPattern = {2.0f, 6.0f};
                return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 10.0f, dotPattern, 0.0f);
            case SOLID:
            default:
                return new BasicStroke(width);
        }
    }

    private void renderTrail(Graphics2D graphics) {
        java.util.List<ExactPathPreviewPlugin.TrailTile> trail = plugin.getRecentPath();
        if (trail.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        Color sparkleColor = config.trailColor();
        Color fillColor = config.trailFillColor();
        Color borderColor = config.trailBorderColor();
        int sparkleSize = config.trailSparkleSize();

        for (ExactPathPreviewPlugin.TrailTile trailTile : trail) {
            if (trailTile.point.getPlane() != client.getPlane()) continue;

            LocalPoint lp = LocalPoint.fromWorld(client, trailTile.point);
            if (lp == null) continue;

            Polygon poly = Perspective.getCanvasTilePoly(client, lp);
            if (poly == null) continue;

            // Calculate fade
            float age = (currentTime - trailTile.timestamp) / (float) config.trailDuration();
            float alpha = config.trailFadeOut() ? 1.0f - age : 1.0f;

            // Fill tile
            if (config.trailFillTiles()) {
                Color fadedFill = new Color(
                        fillColor.getRed(),
                        fillColor.getGreen(),
                        fillColor.getBlue(),
                        (int) (fillColor.getAlpha() * alpha)
                );
                graphics.setColor(fadedFill);
                graphics.fillPolygon(poly);
            }

            // Draw border
            if (config.trailBorder()) {
                Color fadedBorder = new Color(
                        borderColor.getRed(),
                        borderColor.getGreen(),
                        borderColor.getBlue(),
                        (int) (borderColor.getAlpha() * alpha)
                );
                graphics.setColor(fadedBorder);
                graphics.setStroke(new BasicStroke(config.trailBorderWidth()));
                graphics.drawPolygon(poly);
            }

            // Draw sparkles
            Color fadedSparkle = new Color(
                    sparkleColor.getRed(),
                    sparkleColor.getGreen(),
                    sparkleColor.getBlue(),
                    (int) (sparkleColor.getAlpha() * alpha)
            );

            int centerX = poly.getBounds().x + poly.getBounds().width / 2;
            int centerY = poly.getBounds().y + poly.getBounds().height / 2;

            graphics.setColor(fadedSparkle);

            // Draw 4 sparkles per tile
            for (int i = 0; i < 4; i++) {
                int offsetX = (int) ((Math.random() - 0.5) * poly.getBounds().width * 0.6);
                int offsetY = (int) ((Math.random() - 0.5) * poly.getBounds().height * 0.6);
                graphics.fillOval(centerX + offsetX, centerY + offsetY, sparkleSize, sparkleSize);
            }
        }
    }
}
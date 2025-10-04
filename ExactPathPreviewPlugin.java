package net.runelite.client.plugins.exactpathpreview;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.Polygon;
import java.util.*;

@Slf4j
@PluginDescriptor(
        name = "Exact Path Preview",
        description = "Shows the EXACT path your character will take using OSRS pathfinding algorithm",
        tags = {"path", "walking", "navigation", "overlay", "pathfinding"}
)
public class ExactPathPreviewPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private ExactPathPreviewConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ExactPathPreviewOverlay overlay;

    @Inject
    private ExactPathPreviewInfoOverlay infoOverlay;

    @Getter
    private List<WorldPoint> currentPath = new ArrayList<>();

    @Getter
    private WorldPoint hoveredTile = null;

    private WorldPoint lastHoveredTile = null;

    @Getter
    private long lastCalculationTime = 0;

    // Cache collision data to avoid copying every frame
    private int[][] cachedCollisionData = null;
    private int cachedCollisionPlane = -1;

    // Trail tracking
    private final LinkedList<TrailTile> recentPath = new LinkedList<>();
    private WorldPoint lastPlayerPosition = null;

    // OSRS pathfinding constants
    private static final int MAX_DISTANCE = 128;
    private static final int FALLBACK_SEARCH_SIZE = 21;
    private static final int MAX_PATH_LENGTH = 100;
    private static final int REACHABILITY_CHECK = 50; // 101x101 grid

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        overlayManager.add(infoOverlay);
        log.info("Exact Path Preview started!");
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        overlayManager.remove(infoOverlay);
        currentPath.clear();
        hoveredTile = null;
        lastHoveredTile = null;
        cachedCollisionData = null;
        cachedCollisionPlane = -1;
        log.info("Exact Path Preview stopped!");
    }

    @Subscribe
    public void onClientTick(ClientTick event) {
        if (!config.enabled()) {
            if (!currentPath.isEmpty()) {
                currentPath.clear();
                hoveredTile = null;
                lastHoveredTile = null;
                recentPath.clear();
            }
            return;
        }

        // Update trail
        if (config.showTrail()) {
            updateTrail();
        }

        // Get the hovered tile using client methods - runs every frame!
        Tile selectedTile = getSelectedSceneTile();

        if (selectedTile != null) {
            WorldPoint hoveredWorld = WorldPoint.fromLocalInstance(client, selectedTile.getLocalLocation());
            if (hoveredWorld != null) {
                hoveredTile = hoveredWorld;

                if (lastHoveredTile == null || !hoveredWorld.equals(lastHoveredTile)) {
                    lastHoveredTile = hoveredWorld;
                    calculatePath(hoveredWorld);
                }
            }
        } else {
            if (lastHoveredTile != null) {
                lastHoveredTile = null;
                hoveredTile = null;
                currentPath.clear();
            }
        }
    }

    /**
     * Get the currently selected/hovered scene tile
     * Optimized for fast PvM - caches polygon calculations
     */
    private Tile getSelectedSceneTile() {
        Scene scene = client.getScene();
        if (scene == null) {
            return null;
        }

        Tile[][][] tiles = scene.getTiles();
        int plane = client.getPlane();

        if (tiles == null || plane < 0 || plane >= tiles.length) {
            return null;
        }

        // Get mouse position
        net.runelite.api.Point mouseCanvasPos = client.getMouseCanvasPosition();
        int mouseX = mouseCanvasPos.getX();
        int mouseY = mouseCanvasPos.getY();

        if (mouseX < 0 || mouseY < 0) {
            return null;
        }

        // Optimized search - check tiles near viewport center first
        // This is faster for PvM where you're usually looking at center screen
        Tile[][] planeTiles = tiles[plane];
        int midX = planeTiles.length / 2;
        int midY = planeTiles[0].length / 2;
        int searchRadius = Math.max(planeTiles.length, planeTiles[0].length);

        for (int radius = 0; radius < searchRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius) {
                        continue; // Only check perimeter of current radius
                    }

                    int x = midX + dx;
                    int y = midY + dy;

                    if (x < 0 || y < 0 || x >= planeTiles.length || y >= planeTiles[x].length) {
                        continue;
                    }

                    Tile tile = planeTiles[x][y];
                    if (tile == null) {
                        continue;
                    }

                    Polygon poly = Perspective.getCanvasTilePoly(client, tile.getLocalLocation());
                    if (poly != null && poly.contains(mouseX, mouseY)) {
                        return tile;
                    }
                }
            }
        }

        return null;
    }

    private void calculatePath(WorldPoint destination) {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            currentPath.clear();
            return;
        }

        long startTime = System.nanoTime();

        WorldPoint start = localPlayer.getWorldLocation();
        currentPath = findPathOSRS(start, destination);

        lastCalculationTime = System.nanoTime() - startTime;

        if (config.debugMode()) {
            log.debug("Path calculated in {} μs. Path length: {}",
                    lastCalculationTime / 1000, currentPath.size());
        }
    }

    /**
     * OSRS Breadth-First Search pathfinding algorithm
     * This matches the actual game's pathfinding exactly
     */
    private List<WorldPoint> findPathOSRS(WorldPoint start, WorldPoint end) {
        if (start.equals(end)) {
            return Collections.emptyList();
        }

        int plane = start.getPlane();
        if (plane != end.getPlane()) {
            return Collections.emptyList();
        }

        // Check if destination is within 101x101 area (reachability check)
        int dx = Math.abs(end.getX() - start.getX());
        int dy = Math.abs(end.getY() - start.getY());
        if (dx > REACHABILITY_CHECK || dy > REACHABILITY_CHECK) {
            return Collections.emptyList();
        }

        int[][] collisionData = getCollisionData(plane);
        if (collisionData == null) {
            return Collections.emptyList();
        }

        // BFS data structures
        Queue<PathNode> queue = new LinkedList<>();
        Map<WorldPoint, PathNode> visited = new HashMap<>();

        PathNode startNode = new PathNode(start, null, 0);
        queue.add(startNode);
        visited.put(start, startNode);

        PathNode targetNode = null;

        // BFS - exact OSRS order: W, E, S, N, SW, SE, NW, NE
        int[][] directions = {
                {-1, 0},  // West
                {1, 0},   // East
                {0, -1},  // South
                {0, 1},   // North
                {-1, -1}, // South-west
                {1, -1},  // South-east
                {-1, 1},  // North-west
                {1, 1}    // North-east
        };

        while (!queue.isEmpty()) {
            PathNode current = queue.poll();

            // Check if we reached destination
            if (current.point.equals(end)) {
                targetNode = current;
                break;
            }

            // Check all 8 directions in OSRS order
            for (int[] dir : directions) {
                WorldPoint neighbor = new WorldPoint(
                        current.point.getX() + dir[0],
                        current.point.getY() + dir[1],
                        plane
                );

                // Must be within 128x128 grid centered at start
                int distX = Math.abs(neighbor.getX() - start.getX());
                int distY = Math.abs(neighbor.getY() - start.getY());
                if (distX >= MAX_DISTANCE || distY >= MAX_DISTANCE) {
                    continue;
                }

                if (visited.containsKey(neighbor)) {
                    continue;
                }

                // Check if movement is valid using collision data
                if (!canTraverse(current.point, neighbor, dir[0], dir[1], collisionData)) {
                    continue;
                }

                PathNode neighborNode = new PathNode(neighbor, current, current.distance + 1);
                visited.put(neighbor, neighborNode);
                queue.add(neighborNode);
            }
        }

        // If no path found to exact tile, try fallback search
        if (targetNode == null && config.showFallbackPath()) {
            targetNode = fallbackSearch(end, visited);
        }

        if (targetNode == null) {
            return Collections.emptyList();
        }

        // Reconstruct full path
        List<WorldPoint> fullPath = reconstructPath(targetNode);

        // Extract checkpoint tiles (corners) - max 25
        if (config.showCheckpointsOnly()) {
            return extractCheckpoints(fullPath);
        }

        return fullPath;
    }

    /**
     * OSRS fallback search when exact tile can't be reached
     */
    private PathNode fallbackSearch(WorldPoint target, Map<WorldPoint, PathNode> visited) {
        PathNode bestNode = null;
        int bestDistance = Integer.MAX_VALUE;
        double bestEuclidean = Double.MAX_VALUE;

        // Search 21x21 grid centered at target (western then southern priority)
        for (int dx = -FALLBACK_SEARCH_SIZE / 2; dx <= FALLBACK_SEARCH_SIZE / 2; dx++) {
            for (int dy = -FALLBACK_SEARCH_SIZE / 2; dy <= FALLBACK_SEARCH_SIZE / 2; dy++) {
                WorldPoint checkPoint = new WorldPoint(
                        target.getX() + dx,
                        target.getY() + dy,
                        target.getPlane()
                );

                PathNode node = visited.get(checkPoint);
                if (node == null || node.distance >= MAX_PATH_LENGTH) {
                    continue;
                }

                double euclidean = Math.sqrt(dx * dx + dy * dy);

                // Find shortest path distance, then closest Euclidean
                if (node.distance < bestDistance ||
                        (node.distance == bestDistance && euclidean < bestEuclidean)) {
                    bestNode = node;
                    bestDistance = node.distance;
                    bestEuclidean = euclidean;
                }
            }
        }

        return bestNode;
    }

    /**
     * Extract checkpoint tiles (corners of path) - max 25
     */
    private List<WorldPoint> extractCheckpoints(List<WorldPoint> path) {
        if (path.size() <= 2) {
            return path;
        }

        List<WorldPoint> checkpoints = new ArrayList<>();
        checkpoints.add(path.get(0));

        for (int i = 1; i < path.size() - 1; i++) {
            WorldPoint prev = path.get(i - 1);
            WorldPoint curr = path.get(i);
            WorldPoint next = path.get(i + 1);

            int dx1 = curr.getX() - prev.getX();
            int dy1 = curr.getY() - prev.getY();
            int dx2 = next.getX() - curr.getX();
            int dy2 = next.getY() - curr.getY();

            // Direction changed - this is a corner/checkpoint
            if (dx1 != dx2 || dy1 != dy2) {
                checkpoints.add(curr);

                // OSRS limits to 25 checkpoints
                if (checkpoints.size() >= 25) {
                    break;
                }
            }
        }

        // Always add final destination
        if (checkpoints.size() < 25) {
            checkpoints.add(path.get(path.size() - 1));
        }

        return checkpoints;
    }

    private boolean canTraverse(WorldPoint from, WorldPoint to, int dx, int dy, int[][] collisionData) {
        int baseX = client.getBaseX();
        int baseY = client.getBaseY();

        int toSceneX = to.getX() - baseX;
        int toSceneY = to.getY() - baseY;

        // Check bounds
        if (toSceneX < 0 || toSceneY < 0 || toSceneX >= 104 || toSceneY >= 104) {
            return false;
        }

        int toFlags = collisionData[toSceneX][toSceneY];

        // Destination must not be fully blocked
        if ((toFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0) {
            return false;
        }

        int fromSceneX = from.getX() - baseX;
        int fromSceneY = from.getY() - baseY;

        if (fromSceneX < 0 || fromSceneY < 0 || fromSceneX >= 104 || fromSceneY >= 104) {
            return false;
        }

        int fromFlags = collisionData[fromSceneX][fromSceneY];

        // Check directional blocking
        boolean diagonal = (dx != 0 && dy != 0);

        if (diagonal) {
            // For diagonal movement, check if blocked diagonally
            if (dx == -1 && dy == -1) { // SW
                if ((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST) != 0) return false;
                if ((toFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST) != 0) return false;
            } else if (dx == 1 && dy == -1) { // SE
                if ((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST) != 0) return false;
                if ((toFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST) != 0) return false;
            } else if (dx == -1 && dy == 1) { // NW
                if ((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST) != 0) return false;
                if ((toFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST) != 0) return false;
            } else if (dx == 1 && dy == 1) { // NE
                if ((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST) != 0) return false;
                if ((toFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST) != 0) return false;
            }

            // Also check adjacent cardinal tiles for diagonal movement
            WorldPoint horizontal = new WorldPoint(to.getX(), from.getY(), from.getPlane());
            WorldPoint vertical = new WorldPoint(from.getX(), to.getY(), from.getPlane());

            int hSceneX = horizontal.getX() - baseX;
            int hSceneY = horizontal.getY() - baseY;
            int vSceneX = vertical.getX() - baseX;
            int vSceneY = vertical.getY() - baseY;

            if (hSceneX >= 0 && hSceneY >= 0 && hSceneX < 104 && hSceneY < 104) {
                int hFlags = collisionData[hSceneX][hSceneY];
                if ((hFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0) return false;
            }

            if (vSceneX >= 0 && vSceneY >= 0 && vSceneX < 104 && vSceneY < 104) {
                int vFlags = collisionData[vSceneX][vSceneY];
                if ((vFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0) return false;
            }
        } else {
            // Cardinal direction
            if (dx == -1) { // West
                if ((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0) return false;
                if ((toFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0) return false;
            } else if (dx == 1) { // East
                if ((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0) return false;
                if ((toFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0) return false;
            } else if (dy == -1) { // South
                if ((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) return false;
                if ((toFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0) return false;
            } else if (dy == 1) { // North
                if ((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0) return false;
                if ((toFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) return false;
            }
        }

        return true;
    }

    private int[][] getCollisionData(int plane) {
        // Use cached collision data if same plane
        if (cachedCollisionData != null && cachedCollisionPlane == plane) {
            return cachedCollisionData;
        }

        CollisionData[] collisionMaps = client.getCollisionMaps();

        if (collisionMaps == null || plane < 0 || plane >= collisionMaps.length || collisionMaps[plane] == null) {
            return null;
        }

        int[][] collisionData = new int[104][104];
        int[][] flags = collisionMaps[plane].getFlags();

        for (int x = 0; x < 104; x++) {
            System.arraycopy(flags[x], 0, collisionData[x], 0, 104);
        }

        // Cache for next frame
        cachedCollisionData = collisionData;
        cachedCollisionPlane = plane;

        return collisionData;
    }

    private List<WorldPoint> reconstructPath(PathNode node) {
        List<WorldPoint> path = new ArrayList<>();
        PathNode current = node;

        while (current != null) {
            path.add(current.point);
            current = current.parent;
        }

        Collections.reverse(path);
        return path;
    }

    @Provides
    ExactPathPreviewConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ExactPathPreviewConfig.class);
    }

    private void updateTrail() {
        Player player = client.getLocalPlayer();
        if (player == null) return;

        WorldPoint currentPos = player.getWorldLocation();

        if (lastPlayerPosition != null && !currentPos.equals(lastPlayerPosition)) {
            recentPath.addFirst(new TrailTile(lastPlayerPosition, System.currentTimeMillis()));

            // Remove old tiles
            int maxTrail = config.trailLength();
            while (recentPath.size() > maxTrail) {
                recentPath.removeLast();
            }

            // Remove expired tiles
            long currentTime = System.currentTimeMillis();
            recentPath.removeIf(tile -> currentTime - tile.timestamp > config.trailDuration());
        }

        lastPlayerPosition = currentPos;
    }

    public List<TrailTile> getRecentPath() {
        return new ArrayList<>(recentPath);
    }

    public static class TrailTile {
        WorldPoint point;
        long timestamp;

        TrailTile(WorldPoint point, long timestamp) {
            this.point = point;
            this.timestamp = timestamp;
        }
    }

    private static class PathNode {
        WorldPoint point;
        PathNode parent;
        int distance;

        PathNode(WorldPoint point, PathNode parent, int distance) {
            this.point = point;
            this.parent = parent;
            this.distance = distance;
        }
    }
}
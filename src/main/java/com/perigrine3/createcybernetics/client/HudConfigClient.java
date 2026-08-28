package com.perigrine3.createcybernetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.perigrine3.createcybernetics.CreateCybernetics;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HudConfigClient {

    private HudConfigClient() {}

    public enum TargetMode {
        ABOVE_HOTBAR,
        UNDER_CROSSHAIR,
        OFF
    }

    public enum BatteryMode {
        TEXT_ONLY,
        ICON_ONLY,
        ICON_PLUS_CAPACITY,
        ICON_PLUS_CAPACITY_PLUS_STATS
    }

    public enum HudComponent {
        HUD_LEFT,
        HUD_RIGHT,
        BATTERY,
        COORDS,
        TOGGLE_LIST,
        SHARDS,
        TARGET,
        MINIMAP
    }

    public static final class ComponentLayout {
        public boolean enabled;
        public float x;
        public float y;
        public float scale;

        public ComponentLayout(boolean enabled, float x, float y, float scale) {
            this.enabled = enabled;
            this.x = x;
            this.y = y;
            this.scale = scale;
            sanitize();
        }

        public ComponentLayout copy() {
            return new ComponentLayout(enabled, x, y, scale);
        }

        public void sanitize() {
            x = clamp(x, 0.0f, 1.0f);
            y = clamp(y, 0.0f, 1.0f);
            scale = clamp(scale, 0.35f, 4.0f);
        }

        public int pixelX(int screenPxW) {
            return Math.round(x * screenPxW);
        }

        public int pixelY(int screenPxH) {
            return Math.round(y * screenPxH);
        }

        public void setFromPixel(int px, int py, int screenPxW, int screenPxH) {
            if (screenPxW <= 0 || screenPxH <= 0) return;

            x = clamp(px / (float) screenPxW, 0.0f, 1.0f);
            y = clamp(py / (float) screenPxH, 0.0f, 1.0f);
        }

        public void adjustScale(float delta) {
            scale = clamp(scale + delta, 0.35f, 4.0f);
        }
    }

    public static final class HudLayerLayout {
        public boolean enabled;
        public float leftX;
        public float rightX;
        public float y;
        public float scale;

        public HudLayerLayout(boolean enabled, float leftX, float rightX, float y, float scale) {
            this.enabled = enabled;
            this.leftX = leftX;
            this.rightX = rightX;
            this.y = y;
            this.scale = scale;
            sanitize();
        }

        public HudLayerLayout copy() {
            return new HudLayerLayout(enabled, leftX, rightX, y, scale);
        }

        public void sanitize() {
            leftX = clamp(leftX, 0.0f, 1.0f);
            rightX = clamp(rightX, 0.0f, 1.0f);
            y = clamp(y, 0.0f, 1.0f);
            scale = clamp(scale, 0.20f, 4.0f);
        }

        public int leftPixelX(int screenPxW) {
            return Math.round(leftX * screenPxW);
        }

        public int rightPixelX(int screenPxW) {
            return Math.round(rightX * screenPxW);
        }

        public int pixelY(int screenPxH) {
            return Math.round(y * screenPxH);
        }

        public void setLeftFromPixel(int px, int screenPxW) {
            if (screenPxW <= 0) return;
            leftX = clamp(px / (float) screenPxW, 0.0f, 1.0f);
        }

        public void setRightFromPixel(int px, int screenPxW) {
            if (screenPxW <= 0) return;
            rightX = clamp(px / (float) screenPxW, 0.0f, 1.0f);
        }

        public void setYFromPixel(int py, int screenPxH) {
            if (screenPxH <= 0) return;
            y = clamp(py / (float) screenPxH, 0.0f, 1.0f);
        }

        public void adjustScale(float delta) {
            scale = clamp(scale + delta, 0.20f, 4.0f);
        }
    }

    public static final class RamLayout {
        public float x;
        public float y;
        public float width;
        public float height;

        public RamLayout(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            sanitize();
        }

        public RamLayout copy() {
            return new RamLayout(x, y, width, height);
        }

        public void sanitize() {
            x = clamp(x, 0.0f, 1.0f);
            y = clamp(y, 0.0f, 1.0f);
            width = clamp(width, 0.01f, 1.0f);
            height = clamp(height, 0.01f, 1.0f);

            if (x + width > 1.0f) {
                x = Math.max(0.0f, 1.0f - width);
            }

            if (y + height > 1.0f) {
                y = Math.max(0.0f, 1.0f - height);
            }
        }

        public int pixelX(int screenW) {
            return Math.round(x * screenW);
        }

        public int pixelY(int screenH) {
            return Math.round(y * screenH);
        }

        public int pixelWidth(int screenW) {
            return Math.max(1, Math.round(width * screenW));
        }

        public int pixelHeight(int screenH) {
            return Math.max(1, Math.round(height * screenH));
        }

        public void setPositionFromPixel(int px, int py, int screenW, int screenH) {
            if (screenW <= 0 || screenH <= 0) return;

            x = clamp(px / (float) screenW, 0.0f, 1.0f);
            y = clamp(py / (float) screenH, 0.0f, 1.0f);

            sanitize();
        }

        public void setSizeFromPixel(int pixelWidth, int pixelHeight, int screenW, int screenH) {
            if (screenW <= 0 || screenH <= 0) return;

            width = clamp(pixelWidth / (float) screenW, 0.01f, 1.0f);
            height = clamp(pixelHeight / (float) screenH, 0.01f, 1.0f);

            sanitize();
        }

        public void setBoundsFromPixel(int px, int py, int pixelWidth, int pixelHeight, int screenW, int screenH) {
            if (screenW <= 0 || screenH <= 0) return;

            x = clamp(px / (float) screenW, 0.0f, 1.0f);
            y = clamp(py / (float) screenH, 0.0f, 1.0f);
            width = clamp(pixelWidth / (float) screenW, 0.01f, 1.0f);
            height = clamp(pixelHeight / (float) screenH, 0.01f, 1.0f);

            sanitize();
        }
    }

    public static final class HudConfig {
        public HudLayerLayout hudLayer;

        public ComponentLayout battery;
        public ComponentLayout coords;
        public ComponentLayout toggleList;
        public ComponentLayout shards;
        public ComponentLayout target;
        public ComponentLayout minimap;

        public RamLayout ram;

        public TargetMode targetMode;
        public BatteryMode batteryMode;

        public HudConfig(HudLayerLayout hudLayer, ComponentLayout battery, ComponentLayout coords, ComponentLayout toggleList, ComponentLayout shards, ComponentLayout target, ComponentLayout minimap, RamLayout ram, TargetMode targetMode, BatteryMode batteryMode) {
            this.hudLayer = hudLayer;
            this.battery = battery;
            this.coords = coords;
            this.toggleList = toggleList;
            this.shards = shards;
            this.target = target;
            this.minimap = minimap;
            this.ram = ram;
            this.targetMode = targetMode;
            this.batteryMode = batteryMode;
            sanitize();
        }

        public HudConfig copy() {
            return new HudConfig(hudLayer.copy(), battery.copy(), coords.copy(), toggleList.copy(), shards.copy(), target.copy(), minimap.copy(), ram.copy(), targetMode, batteryMode);
        }

        public ComponentLayout layout(HudComponent component) {
            return switch (component) {
                case BATTERY -> battery;
                case COORDS -> coords;
                case TOGGLE_LIST -> toggleList;
                case SHARDS -> shards;
                case TARGET -> target;
                case MINIMAP -> minimap;
                case HUD_LEFT, HUD_RIGHT -> throw new IllegalArgumentException("HUD layer uses hudLayer, not ComponentLayout.");
            };
        }

        public boolean enabled(HudComponent component) {
            return switch (component) {
                case HUD_LEFT, HUD_RIGHT -> hudLayer.enabled;
                case BATTERY -> battery.enabled;
                case COORDS -> coords.enabled;
                case TOGGLE_LIST -> toggleList.enabled;
                case SHARDS -> shards.enabled;
                case TARGET -> target.enabled && targetMode != TargetMode.OFF;
                case MINIMAP -> minimap.enabled;
            };
        }

        public void sanitize() {
            if (hudLayer == null) hudLayer = defaultHudLayer();

            if (battery == null) battery = defaultBattery();
            if (coords == null) coords = defaultCoords();
            if (toggleList == null) toggleList = defaultToggleList();
            if (shards == null) shards = defaultShards();
            if (target == null) target = defaultTarget();
            if (minimap == null) minimap = defaultMinimap();
            if (ram == null) ram = defaultRam();

            hudLayer.sanitize();

            battery.sanitize();
            coords.sanitize();
            toggleList.sanitize();
            shards.sanitize();
            target.sanitize();
            minimap.sanitize();
            ram.sanitize();

            if (targetMode == null) targetMode = TargetMode.ABOVE_HOTBAR;
            if (batteryMode == null) batteryMode = BatteryMode.ICON_PLUS_CAPACITY_PLUS_STATS;

            target.enabled = targetMode != TargetMode.OFF;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, HudConfig> CACHE = new ConcurrentHashMap<>();

    private static HudLayerLayout defaultHudLayer() {
        return new HudLayerLayout(true, 0.025f, 0.525f, 0.025f, 1.0f);
    }

    private static ComponentLayout defaultBattery() {
        return new ComponentLayout(true, 0.835f, 0.815f, 1.0f);
    }

    private static ComponentLayout defaultCoords() {
        return new ComponentLayout(true, 0.765f, 0.110f, 1.0f);
    }

    private static ComponentLayout defaultToggleList() {
        return new ComponentLayout(true, 0.070f, 0.120f, 1.0f);
    }

    private static ComponentLayout defaultShards() {
        return new ComponentLayout(true, 0.135f, 0.835f, 1.0f);
    }

    private static ComponentLayout defaultTarget() {
        return new ComponentLayout(true, 0.500f, 0.735f, 1.0f);
    }

    private static ComponentLayout defaultMinimap() {
        return new ComponentLayout(true, 0.865f, 0.025f, 1.0f);
    }

    private static RamLayout defaultRam() {
        return new RamLayout(0.250f, 0.015f, 0.500f, 0.100f);
    }

    public static HudConfig defaultConfig() {
        return new HudConfig(defaultHudLayer(), defaultBattery(), defaultCoords(), defaultToggleList(), defaultShards(), defaultTarget(), defaultMinimap(), defaultRam(), TargetMode.ABOVE_HOTBAR, BatteryMode.ICON_PLUS_CAPACITY_PLUS_STATS);
    }

    private static Path fileFor(UUID playerId) {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(CreateCybernetics.MODID);
        return dir.resolve("hud_layout_" + playerId + ".json");
    }

    public static HudConfig get(UUID playerId) {
        return CACHE.computeIfAbsent(playerId, HudConfigClient::loadOrDefault);
    }

    public static void reload(UUID playerId) {
        CACHE.put(playerId, loadOrDefault(playerId));
    }

    public static void invalidate(UUID playerId) {
        CACHE.remove(playerId);
    }

    public static void reset(UUID playerId) {
        try {
            Files.deleteIfExists(fileFor(playerId));
        } catch (Throwable ignored) {
        }

        CACHE.put(playerId, defaultConfig());
    }

    public static void save(UUID playerId, HudConfig cfg) {
        if (cfg == null) return;

        cfg.sanitize();
        CACHE.put(playerId, cfg.copy());

        Path file = fileFor(playerId);

        try {
            Files.createDirectories(file.getParent());

            JsonObject root = new JsonObject();

            root.add("hudLayer", writeHudLayer(cfg.hudLayer));

            root.add("battery", writeLayout(cfg.battery));
            root.add("coords", writeLayout(cfg.coords));
            root.add("toggleList", writeLayout(cfg.toggleList));
            root.add("shards", writeLayout(cfg.shards));
            root.add("target", writeLayout(cfg.target));
            root.add("minimap", writeLayout(cfg.minimap));
            root.add("ram", writeRamLayout(cfg.ram));

            root.addProperty("targetMode", cfg.targetMode.name());
            root.addProperty("batteryMode", cfg.batteryMode.name());

            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(root, w);
            }
        } catch (Throwable ignored) {
        }
    }

    private static HudConfig loadOrDefault(UUID playerId) {
        Path file = fileFor(playerId);
        if (!Files.exists(file)) return defaultConfig();

        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject obj = GSON.fromJson(r, JsonObject.class);
            if (obj == null) return defaultConfig();

            HudConfig cfg = defaultConfig();

            cfg.hudLayer = readHudLayer(obj, "hudLayer", cfg.hudLayer);

            cfg.battery = readLayout(obj, "battery", cfg.battery);
            cfg.coords = readLayout(obj, "coords", cfg.coords);
            cfg.toggleList = readLayout(obj, "toggleList", cfg.toggleList);
            cfg.shards = readLayout(obj, "shards", cfg.shards);
            cfg.target = readLayout(obj, "target", cfg.target);
            cfg.minimap = readLayout(obj, "minimap", cfg.minimap);
            cfg.ram = readRamLayout(obj, "ram", cfg.ram);

            if (obj.has("targetMode")) {
                cfg.targetMode = parseTargetMode(obj.get("targetMode").getAsString());
            }

            if (obj.has("batteryMode")) {
                cfg.batteryMode = parseBatteryMode(obj.get("batteryMode").getAsString());
            }

            if (obj.has("coordsEnabled")) {
                cfg.coords.enabled = obj.get("coordsEnabled").getAsBoolean();
            }

            if (obj.has("toggleListEnabled")) {
                cfg.toggleList.enabled = obj.get("toggleListEnabled").getAsBoolean();
            }

            if (obj.has("shardsEnabled")) {
                cfg.shards.enabled = obj.get("shardsEnabled").getAsBoolean();
            }

            cfg.sanitize();
            return cfg;
        } catch (Throwable ignored) {
            return defaultConfig();
        }
    }

    private static JsonObject writeLayout(ComponentLayout layout) {
        layout.sanitize();

        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", layout.enabled);
        obj.addProperty("x", layout.x);
        obj.addProperty("y", layout.y);
        obj.addProperty("scale", layout.scale);
        return obj;
    }

    private static ComponentLayout readLayout(JsonObject root, String key, ComponentLayout fallback) {
        if (!root.has(key) || !root.get(key).isJsonObject()) {
            return fallback.copy();
        }

        JsonObject obj = root.getAsJsonObject(key);
        ComponentLayout out = fallback.copy();

        try {
            if (obj.has("enabled")) out.enabled = obj.get("enabled").getAsBoolean();
            if (obj.has("x")) out.x = obj.get("x").getAsFloat();
            if (obj.has("y")) out.y = obj.get("y").getAsFloat();
            if (obj.has("scale")) out.scale = obj.get("scale").getAsFloat();
        } catch (Throwable ignored) {
            return fallback.copy();
        }

        out.sanitize();
        return out;
    }

    private static JsonObject writeRamLayout(RamLayout layout) {
        layout.sanitize();

        JsonObject obj = new JsonObject();
        obj.addProperty("x", layout.x);
        obj.addProperty("y", layout.y);
        obj.addProperty("width", layout.width);
        obj.addProperty("height", layout.height);
        return obj;
    }

    private static RamLayout readRamLayout(JsonObject root, String key, RamLayout fallback) {
        if (!root.has(key) || !root.get(key).isJsonObject()) {
            return fallback.copy();
        }

        JsonObject obj = root.getAsJsonObject(key);
        RamLayout out = fallback.copy();

        try {
            if (obj.has("x")) out.x = obj.get("x").getAsFloat();
            if (obj.has("y")) out.y = obj.get("y").getAsFloat();
            if (obj.has("width")) out.width = obj.get("width").getAsFloat();
            if (obj.has("height")) out.height = obj.get("height").getAsFloat();
        } catch (Throwable ignored) {
            return fallback.copy();
        }

        out.sanitize();
        return out;
    }

    private static JsonObject writeHudLayer(HudLayerLayout layout) {
        layout.sanitize();

        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", layout.enabled);
        obj.addProperty("leftX", layout.leftX);
        obj.addProperty("rightX", layout.rightX);
        obj.addProperty("y", layout.y);
        obj.addProperty("scale", layout.scale);
        return obj;
    }

    private static HudLayerLayout readHudLayer(JsonObject root, String key, HudLayerLayout fallback) {
        if (!root.has(key) || !root.get(key).isJsonObject()) {
            return fallback.copy();
        }

        JsonObject obj = root.getAsJsonObject(key);
        HudLayerLayout out = fallback.copy();

        try {
            if (obj.has("enabled")) out.enabled = obj.get("enabled").getAsBoolean();
            if (obj.has("leftX")) out.leftX = obj.get("leftX").getAsFloat();
            if (obj.has("rightX")) out.rightX = obj.get("rightX").getAsFloat();
            if (obj.has("y")) out.y = obj.get("y").getAsFloat();
            if (obj.has("scale")) out.scale = obj.get("scale").getAsFloat();
        } catch (Throwable ignored) {
            return fallback.copy();
        }

        out.sanitize();
        return out;
    }

    private static TargetMode parseTargetMode(@Nullable String s) {
        if (s == null) return TargetMode.ABOVE_HOTBAR;

        try {
            return TargetMode.valueOf(s);
        } catch (Throwable ignored) {
            return TargetMode.ABOVE_HOTBAR;
        }
    }

    private static BatteryMode parseBatteryMode(@Nullable String s) {
        if (s == null) return BatteryMode.ICON_PLUS_CAPACITY_PLUS_STATS;

        try {
            return BatteryMode.valueOf(s);
        } catch (Throwable ignored) {
            return BatteryMode.ICON_PLUS_CAPACITY_PLUS_STATS;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
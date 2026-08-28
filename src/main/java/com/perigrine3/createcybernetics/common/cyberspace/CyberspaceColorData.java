package com.perigrine3.createcybernetics.common.cyberspace;

import net.minecraft.world.entity.player.Player;

public final class CyberspaceColorData {

    public static final String NBT_COLOR = "createcybernetics_cyberspace_color";
    public static final int DEFAULT_COLOR = 0xFF00D9FF;

    private CyberspaceColorData() {}

    public static int get(Player player) {
        if (player == null) return DEFAULT_COLOR;

        if (!player.getPersistentData().contains(NBT_COLOR)) {
            return DEFAULT_COLOR;
        }

        return sanitize(player.getPersistentData().getInt(NBT_COLOR));
    }

    public static void set(Player player, int color) {
        if (player == null) return;

        player.getPersistentData().putInt(NBT_COLOR, sanitize(color));
    }

    public static int sanitize(int color) {
        return 0xFF000000 | (color & 0x00FFFFFF);
    }
}
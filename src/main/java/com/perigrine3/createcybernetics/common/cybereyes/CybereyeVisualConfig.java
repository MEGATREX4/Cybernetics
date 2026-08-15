package com.perigrine3.createcybernetics.common.cybereyes;

import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.EnumMap;

public final class CybereyeVisualConfig {

    private CybereyeVisualConfig() {}

    public static final int HEAD_UV_WIDTH = 32;
    public static final int HEAD_UV_HEIGHT = 16;
    public static final int MASK_LONG_COUNT = 8;

    public static final String NBT_ROOT = "cc_cybereye_cfg";
    public static final String NBT_PRIMARY_MASK = "primary_mask";
    public static final String NBT_SECONDARY_MASK = "secondary_mask";
    public static final String NBT_OPTICS = "optics";
    public static final String NBT_X = "x";
    public static final String NBT_Y = "y";

    // Legacy keys retained so old saved layouts can migrate.
    public static final String NBT_LEFT = "left";
    public static final String NBT_RIGHT = "right";
    public static final String NBT_VARIANT = "variant";

    public enum Channel {
        PRIMARY,
        SECONDARY
    }

    public enum OpticsType {
        MONOVISION("monovision"),
        MULTIOPTICS_1("multioptics_1"),
        MULTIOPTICS_2("multioptics_2"),
        MULTIOPTICS_3("multioptics_3"),
        MULTIOPTICS_4("multioptics_4");

        public final String id;

        OpticsType(String id) {
            this.id = id;
        }
    }

    public static final class PixelMask {
        private final long[] bits;

        public PixelMask() {
            this.bits = new long[MASK_LONG_COUNT];
        }

        public PixelMask(long[] bits) {
            this.bits = new long[MASK_LONG_COUNT];

            if (bits != null) {
                System.arraycopy(bits, 0, this.bits, 0, Math.min(bits.length, MASK_LONG_COUNT));
            }

            removeInvalidPixels();
        }

        public boolean get(int u, int v) {
            if (!isSelectableHeadPixel(u, v)) return false;

            int index = index(u, v);
            int longIndex = index >>> 6;
            int bitIndex = index & 63;

            return (bits[longIndex] & (1L << bitIndex)) != 0L;
        }

        public void set(int u, int v, boolean value) {
            if (!isSelectableHeadPixel(u, v)) return;

            int index = index(u, v);
            int longIndex = index >>> 6;
            int bitIndex = index & 63;
            long bit = 1L << bitIndex;

            if (value) {
                bits[longIndex] |= bit;
            } else {
                bits[longIndex] &= ~bit;
            }
        }

        public void clear() {
            Arrays.fill(bits, 0L);
        }

        public boolean isEmpty() {
            for (long value : bits) {
                if (value != 0L) return false;
            }

            return true;
        }

        public long[] copyBits() {
            return Arrays.copyOf(bits, bits.length);
        }

        public PixelMask copy() {
            return new PixelMask(bits);
        }

        private void removeInvalidPixels() {
            for (int v = 0; v < HEAD_UV_HEIGHT; v++) {
                for (int u = 0; u < HEAD_UV_WIDTH; u++) {
                    if (isSelectableHeadPixel(u, v)) continue;

                    int index = index(u, v);
                    int longIndex = index >>> 6;
                    int bitIndex = index & 63;

                    bits[longIndex] &= ~(1L << bitIndex);
                }
            }
        }
    }

    public static final class Offset {
        public int x;
        public int y;

        public Offset() {
            this(0, 0);
        }

        public Offset(int x, int y) {
            this.x = clampOffset(x);
            this.y = clampOffset(y);
        }

        public Offset copy() {
            return new Offset(x, y);
        }
    }

    public static final class Layout {
        public final PixelMask primary;
        public final PixelMask secondary;
        public final EnumMap<OpticsType, Offset> optics;

        public Layout() {
            this.primary = new PixelMask();
            this.secondary = new PixelMask();
            this.optics = new EnumMap<>(OpticsType.class);

            for (OpticsType type : OpticsType.values()) {
                optics.put(type, new Offset());
            }
        }

        public Layout(PixelMask primary, PixelMask secondary, EnumMap<OpticsType, Offset> optics) {
            this.primary = primary == null ? new PixelMask() : primary.copy();
            this.secondary = secondary == null ? new PixelMask() : secondary.copy();
            this.optics = new EnumMap<>(OpticsType.class);

            for (OpticsType type : OpticsType.values()) {
                Offset offset = optics == null ? null : optics.get(type);
                this.optics.put(type, offset == null ? new Offset() : offset.copy());
            }
        }

        public Offset getOffset(OpticsType type) {
            return optics.computeIfAbsent(type, ignored -> new Offset());
        }

        public Layout copy() {
            return new Layout(primary, secondary, optics);
        }
    }

    public static Layout defaults() {
        Layout layout = new Layout();

        layout.primary.set(10, 12, true);
        layout.secondary.set(13, 12, true);

        return layout;
    }

    public static boolean isSelectableHeadPixel(int u, int v) {
        if (u < 0 || u >= HEAD_UV_WIDTH) return false;
        if (v < 0 || v >= HEAD_UV_HEIGHT) return false;

        // Head top + bottom.
        if (v < 8) {
            return u >= 8 && u < 24;
        }

        // Right, front, left and back head faces.
        return true;
    }

    public static CompoundTag writeToNbt(Layout layout) {
        Layout safe = layout == null ? defaults() : layout;

        CompoundTag root = new CompoundTag();
        root.putLongArray(NBT_PRIMARY_MASK, safe.primary.copyBits());
        root.putLongArray(NBT_SECONDARY_MASK, safe.secondary.copyBits());

        CompoundTag opticsTag = new CompoundTag();

        for (OpticsType type : OpticsType.values()) {
            Offset offset = safe.getOffset(type);

            CompoundTag entry = new CompoundTag();
            entry.putInt(NBT_X, clampOffset(offset.x));
            entry.putInt(NBT_Y, clampOffset(offset.y));

            opticsTag.put(type.id, entry);
        }

        root.put(NBT_OPTICS, opticsTag);

        return root;
    }

    public static Layout readFromNbt(CompoundTag root) {
        if (root == null || root.isEmpty()) {
            return defaults();
        }

        if (root.contains(NBT_PRIMARY_MASK) || root.contains(NBT_SECONDARY_MASK)) {
            return readModernNbt(root);
        }

        return readLegacyNbt(root);
    }

    private static Layout readModernNbt(CompoundTag root) {
        Layout layout = new Layout();

        copyMask(new PixelMask(root.getLongArray(NBT_PRIMARY_MASK)), layout.primary);
        copyMask(new PixelMask(root.getLongArray(NBT_SECONDARY_MASK)), layout.secondary);

        CompoundTag opticsTag = root.getCompound(NBT_OPTICS);

        for (OpticsType type : OpticsType.values()) {
            CompoundTag entry = opticsTag.getCompound(type.id);
            if (entry.isEmpty()) continue;

            layout.optics.put(type, new Offset(entry.getInt(NBT_X), entry.getInt(NBT_Y)));
        }

        if (layout.primary.isEmpty() && layout.secondary.isEmpty()) {
            Layout defaults = defaults();
            copyMask(defaults.primary, layout.primary);
            copyMask(defaults.secondary, layout.secondary);
        }

        return layout;
    }

    private static Layout readLegacyNbt(CompoundTag root) {
        Layout layout = new Layout();

        readLegacyEye(root.getCompound(NBT_LEFT), layout.primary, 10, 12);
        readLegacyEye(root.getCompound(NBT_RIGHT), layout.secondary, 13, 12);

        return layout;
    }

    private static void readLegacyEye(CompoundTag tag, PixelMask target, int defaultX, int defaultY) {
        if (tag == null || tag.isEmpty()) {
            target.set(defaultX, defaultY, true);
            return;
        }

        int x = tag.getInt(NBT_X);
        int y = tag.getInt(NBT_Y);
        int variant = tag.getInt(NBT_VARIANT);

        int width = variant == 2 ? 2 : 1;
        int height = variant == 0 ? 1 : 2;

        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                target.set(x + dx, y + dy, true);
            }
        }
    }

    public static int clampOffset(int value) {
        return Math.max(-16, Math.min(16, value));
    }

    private static int index(int u, int v) {
        return v * HEAD_UV_WIDTH + u;
    }

    private static void copyMask(PixelMask source, PixelMask target) {
        target.clear();

        for (int v = 0; v < HEAD_UV_HEIGHT; v++) {
            for (int u = 0; u < HEAD_UV_WIDTH; u++) {
                target.set(u, v, source.get(u, v));
            }
        }
    }
}
package com.perigrine3.createcybernetics.compat.vampirism;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class VampirismCompat {

    private static final String VAMPIRISM_MOD_ID = "vampirism";

    private static final String VAMPIRISM_HELPER_CLASS =
            "de.teamlapen.vampirism.util.Helper";

    private static final String VAMPIRE_PLAYER_CLASS =
            "de.teamlapen.vampirism.entity.player.vampire.VampirePlayer";

    private static final String BLOOD_STATS_CLASS =
            "de.teamlapen.vampirism.entity.player.vampire.BloodStats";

    private static final boolean VAMPIRISM_LOADED =
            ModList.get().isLoaded(VAMPIRISM_MOD_ID);

    private static Method isVampireMethod;
    private static boolean isVampireMethodResolved;

    private static Method vampirePlayerGetMethod;
    private static Method getBloodStatsMethod;
    private static Method getBloodLevelMethod;
    private static Method getMaxBloodMethod;

    private static Field bloodSaturationLevelField;
    private static Field bloodTimerField;

    private static boolean regenerationReflectionResolved;

    private VampirismCompat() {}

    public static boolean isLoaded() {
        return VAMPIRISM_LOADED;
    }

    public static boolean isVampire(Entity entity) {
        if (!VAMPIRISM_LOADED) return false;
        if (entity == null) return false;

        Method method = getIsVampireMethod();
        if (method == null) return false;

        try {
            Object result = method.invoke(null, entity);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean isBloodRegenerationHeal(Player player, float healAmount) {
        if (!VAMPIRISM_LOADED) return false;
        if (player == null) return false;
        if (!isVampire(player)) return false;

        if (!resolveRegenerationReflection()) return false;

        try {
            Object vampirePlayer = vampirePlayerGetMethod.invoke(null, player);
            if (vampirePlayer == null) return false;

            Object bloodStats = getBloodStatsMethod.invoke(vampirePlayer);
            if (bloodStats == null) return false;

            Object bloodLevelResult = getBloodLevelMethod.invoke(bloodStats);
            Object maxBloodResult = getMaxBloodMethod.invoke(bloodStats);

            if (!(bloodLevelResult instanceof Integer bloodLevel)) return false;
            if (!(maxBloodResult instanceof Integer maxBlood)) return false;

            float bloodSaturation = bloodSaturationLevelField.getFloat(bloodStats);
            int bloodTimer = bloodTimerField.getInt(bloodStats);

            if (bloodLevel <= 0) return false;

            float levelModifier = getVampireLevelModifier(vampirePlayer);

            if (bloodSaturation > 0.0F && bloodLevel >= maxBlood && bloodTimer >= 10) {
                float saturationUsed = Math.min(bloodSaturation, 6.0F);
                float expectedHeal = (saturationUsed / 6.0F) * levelModifier;
                return approximatelyEqual(healAmount, expectedHeal);
            }

            if (bloodLevel >= 18 && bloodTimer >= 80) {
                return approximatelyEqual(healAmount, levelModifier);
            }

            if (bloodTimer >= 300) {
                return approximatelyEqual(healAmount, 0.5F * levelModifier);
            }

            return false;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static float getVampireLevelModifier(Object vampirePlayer) throws ReflectiveOperationException {
        Method getLevelMethod = vampirePlayer.getClass().getMethod("getLevel");
        Method getMaxLevelMethod = vampirePlayer.getClass().getMethod("getMaxLevel");

        Object levelResult = getLevelMethod.invoke(vampirePlayer);
        Object maxLevelResult = getMaxLevelMethod.invoke(vampirePlayer);

        if (!(levelResult instanceof Integer level)) return 1.0F;
        if (!(maxLevelResult instanceof Integer maxLevel)) return 1.0F;
        if (maxLevel <= 0) return 1.0F;

        return 1.0F + level / (float) maxLevel * 0.5F;
    }

    private static boolean approximatelyEqual(float a, float b) {
        return Math.abs(a - b) <= 0.001F;
    }

    private static Method getIsVampireMethod() {
        if (isVampireMethodResolved) {
            return isVampireMethod;
        }

        isVampireMethodResolved = true;

        try {
            Class<?> helperClass = Class.forName(
                    VAMPIRISM_HELPER_CLASS,
                    false,
                    VampirismCompat.class.getClassLoader()
            );

            isVampireMethod = helperClass.getMethod(
                    "isVampire",
                    Entity.class
            );
        } catch (ReflectiveOperationException | LinkageError ignored) {
            isVampireMethod = null;
        }

        return isVampireMethod;
    }

    private static boolean resolveRegenerationReflection() {
        if (regenerationReflectionResolved) {
            return vampirePlayerGetMethod != null
                    && getBloodStatsMethod != null
                    && getBloodLevelMethod != null
                    && getMaxBloodMethod != null
                    && bloodSaturationLevelField != null
                    && bloodTimerField != null;
        }

        regenerationReflectionResolved = true;

        try {
            ClassLoader classLoader = VampirismCompat.class.getClassLoader();

            Class<?> vampirePlayerClass = Class.forName(
                    VAMPIRE_PLAYER_CLASS,
                    false,
                    classLoader
            );

            Class<?> bloodStatsClass = Class.forName(
                    BLOOD_STATS_CLASS,
                    false,
                    classLoader
            );

            vampirePlayerGetMethod = vampirePlayerClass.getMethod(
                    "get",
                    Player.class
            );

            getBloodStatsMethod = vampirePlayerClass.getMethod(
                    "getBloodStats"
            );

            getBloodLevelMethod = bloodStatsClass.getMethod(
                    "getBloodLevel"
            );

            getMaxBloodMethod = bloodStatsClass.getMethod(
                    "getMaxBlood"
            );

            bloodSaturationLevelField = bloodStatsClass.getDeclaredField(
                    "bloodSaturationLevel"
            );

            bloodTimerField = bloodStatsClass.getDeclaredField(
                    "bloodTimer"
            );

            bloodSaturationLevelField.setAccessible(true);
            bloodTimerField.setAccessible(true);

            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            vampirePlayerGetMethod = null;
            getBloodStatsMethod = null;
            getBloodLevelMethod = null;
            getMaxBloodMethod = null;
            bloodSaturationLevelField = null;
            bloodTimerField = null;
            return false;
        }
    }
}
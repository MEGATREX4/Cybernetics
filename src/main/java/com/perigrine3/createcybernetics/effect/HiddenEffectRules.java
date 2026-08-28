package com.perigrine3.createcybernetics.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public final class HiddenEffectRules {
    private HiddenEffectRules() {
    }

    /*
     * This is the only list of effects hidden by Cybernetics.
     *
     * Both the client inventory extension and the server-side biomonitor
     * scanner use this exact array. Do not duplicate this list elsewhere.
     */
    private static final MobEffect[] HIDDEN_EFFECTS = {
            ModEffects.CYBERPSYCHOSIS_FUGUE.value(),

            ModEffects.AEROSTASIS_GYROBLADDER_EFFECT.value(),
            ModEffects.SYNTHETIC_SETULES_EFFECT.value(),
            ModEffects.PNEUMATIC_CALVES_EFFECT.value(),
            ModEffects.SPURS_EFFECT.value(),
            ModEffects.NEURAL_CONTEXTUALIZER_EFFECT.value(),
            ModEffects.SUBDERMAL_SPIKES_EFFECT.value(),
            ModEffects.GUARDIAN_EYE_EFFECT.value(),
            ModEffects.PROJECTILE_DODGE_EFFECT.value(),
            ModEffects.BREATHLESS_EFFECT.value(),
            ModEffects.SCULK_LUNGS_EFFECT.value(),
            ModEffects.SANDEVISTAN_EFFECT.value(),
            ModEffects.SPIDER_EYES_EFFECT.value(),
            ModEffects.AXOLOTL_REGEN_EFFECT.value()
    };

    public static MobEffect[] getHiddenEffects() {
        return HIDDEN_EFFECTS;
    }

    public static boolean isHidden(MobEffectInstance instance) {
        if (instance == null) {
            return false;
        }

        return isHidden(instance.getEffect().value());
    }

    public static boolean isHidden(MobEffect effect) {
        if (effect == null) {
            return false;
        }

        for (MobEffect hiddenEffect : HIDDEN_EFFECTS) {
            if (effect == hiddenEffect) {
                return true;
            }
        }

        return false;
    }
}
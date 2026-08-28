package com.perigrine3.createcybernetics.common.quickhack;

import net.minecraft.util.Mth;

public final class QuickhackFunctionSettings {
    private static final int PERSISTENT_DURATION_MULTIPLIER = 5;
    private static final int PERSISTENT_MAX_DURATION_TICKS = 20 * 60 * 5;

    private boolean silent;
    private float durationMultiplier = 1.0F;
    private boolean durationModifierApplied;
    private float intensityMultiplier = 1.0F;
    private float efficiencyMultiplier = 1.0F;
    private int delayTicks;
    private int repeatCount = 1;
    private boolean cascade;
    private boolean persistent;
    private boolean randomize;
    private boolean contagion;
    private QuickhackTargetDomain targetDomain = QuickhackTargetDomain.SYSTEM_ALL;

    public boolean silent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public float durationMultiplier() {
        return durationMultiplier;
    }

    public void multiplyDuration(float multiplier) {
        if (durationModifierApplied) return;

        durationMultiplier = Math.max(0.0F, durationMultiplier * multiplier);
        durationModifierApplied = true;
    }

    public float intensityMultiplier() {
        return intensityMultiplier;
    }

    public void multiplyIntensity(float multiplier) {
        intensityMultiplier = Math.max(0.0F, intensityMultiplier * multiplier);
    }

    public float efficiencyMultiplier() {
        return efficiencyMultiplier;
    }

    public void multiplyEfficiency(float multiplier) {
        efficiencyMultiplier = Math.max(0.0F, efficiencyMultiplier * multiplier);
    }

    public int delayTicks() {
        return delayTicks;
    }

    public void addDelayTicks(int ticks) {
        delayTicks = Math.max(0, delayTicks + ticks);
    }

    public int repeatCount() {
        return repeatCount;
    }

    public void addRepeats(int count) {
        repeatCount = Math.max(1, repeatCount + count);
    }

    public boolean cascade() {
        return cascade;
    }

    public void setCascade(boolean cascade) {
        this.cascade = cascade;
    }

    public boolean persistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        if (!persistent) {
            this.persistent = false;
            return;
        }

        if (durationModifierApplied) return;

        this.persistent = true;
        durationModifierApplied = true;
    }

    public boolean randomize() {
        return randomize;
    }

    public void setRandomize(boolean randomize) {
        this.randomize = randomize;
    }

    public boolean contagion() {
        return contagion;
    }

    public void setContagion(boolean contagion) {
        this.contagion = contagion;
    }

    public QuickhackTargetDomain targetDomain() {
        return targetDomain;
    }

    public void setTargetDomain(QuickhackTargetDomain targetDomain) {
        this.targetDomain = targetDomain == null ? QuickhackTargetDomain.SYSTEM_ALL : targetDomain;
    }

    public int durationTicks(int baseTicks) {
        int durationTicks = Math.max(1, Mth.ceil(baseTicks * durationMultiplier));

        if (!persistent) return durationTicks;

        return Math.min(PERSISTENT_MAX_DURATION_TICKS, durationTicks * PERSISTENT_DURATION_MULTIPLIER);
    }

    public int scaledAmount(int baseAmount) {
        return Math.max(1, Mth.ceil(baseAmount * intensityMultiplier));
    }

    public int scaledResourceCost(int baseAmount) {
        return Math.max(0, Mth.ceil(baseAmount * efficiencyMultiplier));
    }
}
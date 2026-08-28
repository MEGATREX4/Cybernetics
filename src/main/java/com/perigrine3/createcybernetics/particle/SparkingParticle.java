package com.perigrine3.createcybernetics.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class SparkingParticle extends TextureSheetParticle {

    protected SparkingParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        this.lifetime = 5 + this.random.nextInt(4);
        this.quadSize = 0.10F + this.random.nextFloat() * 0.08F;

        this.rCol = 0.65F;
        this.gCol = 0.95F;
        this.bCol = 1.0F;

        this.hasPhysics = false;

        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();

        this.xd *= 0.82D;
        this.yd *= 0.82D;
        this.zd *= 0.82D;

        float life = 1.0F - (this.age / (float) this.lifetime);
        this.quadSize *= 0.86F;
        this.alpha = life;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new SparkingParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
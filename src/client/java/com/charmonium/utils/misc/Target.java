package com.charmonium.utils.misc;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class Target {
    private Vec3d vec;
    private Entity entity;
    private BlockPos blockPos;
    private float additionalY;

    public Target(Vec3d vec) {
        this.vec = vec;
    }

    public Target(Entity entity) {
        this.entity = entity;
    }

    public Target(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public Entity getEntity() {
        return entity;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public float getAdditionalY() {
        return additionalY;
    }

    public void setAdditionalY(float additionalY) {
        this.additionalY = additionalY;
    }

    public Optional<Vec3d> getTarget() {
        if (vec != null) {
            return Optional.of(vec);
        } else if (entity != null) {
            double y = Math.min(entity.getHeight() * 0.85, entity.getHeight() - 0.05);
            return Optional.of(entity.getPos().add(0, y, 0));
        } else if (blockPos != null) {
            return Optional.of(new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        } else {
            return Optional.empty();
        }
    }
}
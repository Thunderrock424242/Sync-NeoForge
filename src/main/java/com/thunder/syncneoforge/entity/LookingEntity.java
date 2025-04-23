package com.thunder.syncneoforge.entity;

public interface LookingEntity {
    default boolean changeLookingEntityLookDirection(double cursorDeltaX, double cursorDeltaY) {
        return false;
    }
}

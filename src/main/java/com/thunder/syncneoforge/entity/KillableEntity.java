package com.thunder.syncneoforge.entity;

public interface KillableEntity {
    default void onKillableEntityDeath() { }

    default boolean updateKillableEntityPostDeath() {
        return false;
    }
}

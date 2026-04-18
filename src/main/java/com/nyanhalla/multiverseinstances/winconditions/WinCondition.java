package com.nyanhalla.multiverseinstances.winconditions;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.core.Instance;
import com.nyanhalla.multiverseinstances.core.InstanceManager;

public abstract class WinCondition {

    protected final MultiverseInstances plugin;
    protected final InstanceManager instanceManager;

    protected WinCondition(MultiverseInstances plugin, InstanceManager instanceManager) {
        this.plugin = plugin;
        this.instanceManager = instanceManager;
    }

    public abstract void initializeInstance(Instance instance);

    public abstract void resetInstance(Instance instance);

    public abstract boolean checkVictory(Instance instance);

    public String getType() {
        String className = getClass().getSimpleName();
        return className.replace("WinCondition", "").toLowerCase();
    }
}

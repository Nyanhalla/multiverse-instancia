package com.nyanhalla.multiverseinstancia.winconditions;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.core.Instance;
import com.nyanhalla.multiverseinstancia.core.InstanceManager;

public abstract class WinCondition {

    protected final MultiverseInstancia plugin;
    protected final InstanceManager instanceManager;

    protected WinCondition(MultiverseInstancia plugin, InstanceManager instanceManager) {
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

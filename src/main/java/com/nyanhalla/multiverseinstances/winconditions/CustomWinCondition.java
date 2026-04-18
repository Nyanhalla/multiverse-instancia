package com.nyanhalla.multiverseinstances.winconditions;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.core.Instance;
import com.nyanhalla.multiverseinstances.core.InstanceManager;

public class CustomWinCondition extends WinCondition {

    public CustomWinCondition(MultiverseInstances plugin, InstanceManager instanceManager) {
        super(plugin, instanceManager);
    }

    @Override
    public void initializeInstance(Instance instance) {
        // Custom win conditions pueden ser definidas via script/config
        plugin.getLogger().info("Custom: Win condition personalizada para " + instance.getId());
    }

    @Override
    public void resetInstance(Instance instance) {
        // Reset custom state
    }

    @Override
    public boolean checkVictory(Instance instance) {
        // Evaluacion custom via config
        return false;
    }
}

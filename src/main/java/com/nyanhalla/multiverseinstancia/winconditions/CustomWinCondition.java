package com.nyanhalla.multiverseinstancia.winconditions;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.core.Instance;
import com.nyanhalla.multiverseinstancia.core.InstanceManager;

public class CustomWinCondition extends WinCondition {

    public CustomWinCondition(MultiverseInstancia plugin, InstanceManager instanceManager) {
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

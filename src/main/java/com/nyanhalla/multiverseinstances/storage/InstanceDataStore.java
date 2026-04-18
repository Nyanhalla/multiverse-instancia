package com.nyanhalla.multiverseinstances.storage;

import com.nyanhalla.multiverseinstances.core.Instance;

import java.util.List;
import java.util.Optional;

public interface InstanceDataStore {

    void save(Instance instance);

    Optional<Instance> load(String instanceId);

    void delete(String instanceId);

    List<Instance> getAll();

    boolean exists(String instanceId);

    void clear();
}

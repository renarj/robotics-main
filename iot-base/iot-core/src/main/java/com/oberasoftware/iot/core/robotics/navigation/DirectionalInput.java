package com.oberasoftware.iot.core.robotics.navigation;

import java.util.Map;
import java.util.Set;

public class DirectionalInput {
    private final Map<String, Double> input;

    public DirectionalInput(Map<String, Double> input) {
        this.input = input;
    }

    public Double getAxis(String axisName) {
        return input.get(axisName);
    }

    public Set<String> getInputAxis() {
        return input.keySet();
    }

    public boolean hasInputAxis(String axis) {
        return input.containsKey(axis);
    }

    @Override
    public String toString() {
        return "DirectionalInput{" +
                "input=" + input +
                '}';
    }
}

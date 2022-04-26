package com.oberasoftware.robo.api.humanoid.joints;

import com.oberasoftware.robo.api.behavioural.Behaviour;

public interface Joint extends Behaviour {
    String getID();

    String getName();

    boolean isInverted();

    String getJointType();

    int getMaxDegrees();

    int getMinDegrees();

    boolean moveTo(int degrees);
}

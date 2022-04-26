package com.oberasoftware.robo.maximus.model;

import com.oberasoftware.robo.api.Robot;
import com.oberasoftware.robo.api.behavioural.BehaviouralRobot;
import com.oberasoftware.robo.api.humanoid.joints.Joint;
import com.oberasoftware.robo.api.servo.Servo;
import com.oberasoftware.robo.api.servo.ServoDriver;

public class JointImpl implements Joint {

    public static final int DEFAULT_MAX_DEGREES = 180;
    public static final int DEFAULT_MIN_DEGREES = -180;

    private final String id;
    private final String name;
    private final String type;
    private final boolean inverted;

    private final int minDegrees;
    private final int maxDegrees;

    private Servo servo;
    private ServoDriver servoDriver;

    public JointImpl(String id, String name, String type, boolean inverted) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.minDegrees = DEFAULT_MIN_DEGREES;
        this.maxDegrees = DEFAULT_MAX_DEGREES;
        this.inverted = inverted;
    }

    public JointImpl(String id, String name, String type, int minDegrees, int maxDegrees, boolean inverted) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.minDegrees = minDegrees;
        this.maxDegrees = maxDegrees;
        this.inverted = inverted;
    }

    @Override
    public void initialize(BehaviouralRobot behaviouralRobot, Robot robotCore) {
        this.servoDriver = robotCore.getServoDriver();
        this.servo = this.servoDriver.getServo(id);
    }

    @Override
    public boolean isInverted() {
        return inverted;
    }

    @Override
    public int getMaxDegrees() {
        return maxDegrees;
    }

    @Override
    public int getMinDegrees() {
        return minDegrees;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getJointType() {
        return type;
    }

    @Override
    public boolean moveTo(int degrees) {
        return false;
    }
}

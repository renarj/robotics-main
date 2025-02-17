package com.oberasoftware.iot.core.robotics.commands;

import com.oberasoftware.iot.core.robotics.servo.ServoCommand;

/**
 * @author Renze de Vries
 */
public class ReadTemperatureCommand implements ServoCommand {
    private final String servoId;

    public ReadTemperatureCommand(String servoId) {
        this.servoId = servoId;
    }

    @Override
    public String getServoId() {
        return servoId;
    }

    @Override
    public String toString() {
        return "ReadTemperatureCommand{" +
                "servoId='" + servoId + '\'' +
                '}';
    }
}

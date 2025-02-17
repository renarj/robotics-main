package com.oberasoftware.robo.dynamixel.protocolv2;

/**
 * @author Renze de Vries
 */
public enum DynamixelV2Address {
    MODEL_NUMBER_L(0x00),
    MODEL_NUMBER_H(0x01),
    VERSION_FIRMWARE(0x06),
    ID(0x07),
    OPERATING_MODE(0x0B),
    LED(0x41),
    BAUD_RATE(0x04),
    RETURN_DELAY_TIME(0x09),
    MAX_POSITION(0x30),
    MIN_POSITION(0x34),
    CCW_ANGLE_LIMIT_L(0x08),
    CCW_ANGLE_LIMIT_H(0x09),
    ALARM_LED(0x11),
    ALARM_SHUTDOWN(0x12),
    HARDWARE_ERROR_STATUS(0x46),
    CURRENT_LIMIT(0x26),
    GOAL_CURRENT(0x66),
    TORGUE_ENABLE(0x40),
    MOVING_SPEED_L(0x20),
    MOVING_SPEED_H(0x21),
    TORGUE_LIMIT_L(0x22),
    TORGUE_LIMIT_H(0x23),
    GOAL_VELOCITY(0x68),
    GOAL_POSITION_L(0x74),
    PROFILE_ACCELERATION(0x6C),
    PROFILE_VELOCITY(0x70),
    PRESENT_CURRENT(0x7E),
    PRESENT_SPEED_L(0x80),
    PRESENT_POSITION_L(0x84),
    MODEL_INFORMATION(0x02),
    CURRENT_VOLTAGE(0x90),
    PRESENT_TEMPERATURE(0x92),
    POWER(0x18),
    LIDAR(0x33);


    private final int address;

    DynamixelV2Address(int address) {
        this.address = address;
    }

    public int getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "DynamixelAddress{" +
                "address=" + address +
                '}';
    }
}

package com.oberasoftware.robo.core.sensors;

import com.google.common.collect.Lists;
import com.oberasoftware.base.event.EventHandler;
import com.oberasoftware.base.event.EventSubscribe;
import com.oberasoftware.base.event.impl.LocalEventBus;
import com.oberasoftware.iot.core.robotics.RobotHardware;
import com.oberasoftware.iot.core.robotics.sensors.DirectPort;
import com.oberasoftware.iot.core.robotics.sensors.SensorDriver;
import com.oberasoftware.iot.core.robotics.servo.ServoDriver;
import com.oberasoftware.iot.core.robotics.servo.events.ServoUpdateEvent;
import com.oberasoftware.iot.core.robotics.commands.ReadPositionAndSpeedCommand;
import com.oberasoftware.iot.core.robotics.commands.ReadTemperatureCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

/**
 * @author Renze de Vries
 */
@Component
public class ServoSensorDriver implements SensorDriver<DirectPort<PositionValue>>, EventHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ServoSensorDriver.class);

    private final LocalEventBus localEventBus;

    private final Map<String, ServoPort> ports = new ConcurrentHashMap<>();

    private static final int SERVO_CHECK_INTERVAL = 250;

    private static final int TEMP_CHECK_INTERVAL = 250;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Autowired
    public ServoSensorDriver(LocalEventBus localEventBus) {
        this.localEventBus = localEventBus;
    }

    @Override
    public List<DirectPort<PositionValue>> getPorts() {
        return Lists.newArrayList(ports.values());
    }

    @Override
    public DirectPort<PositionValue> getPort(String portId) {
        return ports.get(portId);
    }

    @Override
    public void activate(RobotHardware robot, Map<String, String> properties) {
        activate(robot.getServoDriver());
    }

    public void activate(ServoDriver servoDriver) {
        LOG.info("Activating servo driver");
        servoDriver.getServos().forEach(s -> {
            LOG.info("Activating servo port: {}", s.getId());
            ports.put(s.getId(), new ServoPort());
        });

        executorService.submit(() -> {
            while(!Thread.currentThread().isInterrupted()) {
                servoDriver.getServos().forEach(s -> {
                    localEventBus.publishSync(new ReadPositionAndSpeedCommand(s.getId()), TimeUnit.SECONDS, 1);
                    sleepUninterruptibly(SERVO_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
                });
            }
        });

        executorService.submit(() -> {
            while(!Thread.currentThread().isInterrupted()) {
                servoDriver.getServos().forEach(s -> {
                    LOG.debug("Requesting servo temps: {}", s.getId());
                    localEventBus.publishSync(new ReadTemperatureCommand(s.getId()), TimeUnit.SECONDS, 1);
                    sleepUninterruptibly(TEMP_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
                });
            }
        });
    }

    @EventSubscribe
    public void receive(ServoUpdateEvent servoUpdate) {
        LOG.debug("Received servo update: {}", servoUpdate);
        if(ports.containsKey(servoUpdate.getServoId())) {
            LOG.debug("Notifying observer: {}", ports.get(servoUpdate.getServoId()));
            ports.get(servoUpdate.getServoId()).notify(servoUpdate);
        } else {
            LOG.warn("No observer for servo: {}", servoUpdate.getServoId());
        }
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }
}

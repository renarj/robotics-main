package com.oberasoftware.robot.nao.sensors;

import com.aldebaran.qi.Session;
import com.oberasoftware.iot.core.robotics.sensors.DirectPort;
import com.oberasoftware.iot.core.robotics.sensors.PortListener;
import com.oberasoftware.iot.core.robotics.sensors.SensorValue;
import com.oberasoftware.robot.nao.SensorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Renze de Vries
 */
public abstract class NaoMemoryPort<T extends SensorValue> implements DirectPort<T> {
    private static final Logger LOG = LoggerFactory.getLogger(NaoMemoryPort.class);

    private final SensorManager sensorManager;
    private final Session session;

    private List<PortListener<T>> listeners = new CopyOnWriteArrayList<>();

    protected NaoMemoryPort(Session session, SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public SensorManager getSensorManager() {
        return sensorManager;
    }

    @Override
    public void listen(PortListener<T> portListener) {
        listeners.add(portListener);
    }

    public void notify(T value) {
        LOG.debug("Notifying listeners of value: {}", value);
        listeners.forEach(l -> l.receive(value));
    }

    public abstract void close();

    public abstract void initialize();
}

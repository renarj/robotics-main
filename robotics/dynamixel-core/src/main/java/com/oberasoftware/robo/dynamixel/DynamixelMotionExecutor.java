package com.oberasoftware.robo.dynamixel;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.oberasoftware.base.event.impl.LocalEventBus;
import com.oberasoftware.iot.core.robotics.motion.*;
import com.oberasoftware.iot.core.robotics.MotionTask;
import com.oberasoftware.iot.core.robotics.commands.BulkPositionSpeedCommand;
import com.oberasoftware.iot.core.robotics.commands.PositionAndSpeedCommand;
import com.oberasoftware.iot.core.robotics.commands.Scale;
import com.oberasoftware.iot.core.robotics.servo.ServoDataManager;
import com.oberasoftware.iot.core.robotics.servo.ServoProperty;
import com.oberasoftware.robo.core.motion.MotionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Renze de Vries
 */
@Component
public class DynamixelMotionExecutor implements MotionExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(DynamixelMotionExecutor.class);

    private static final int INTERVAL = 1000;

    @Autowired
    private LocalEventBus eventBus;

    @Autowired
    private ServoDataManager dataManager;

    @Autowired
    private MotionManager motionManager;

    @Autowired
    private BulkWriteMovementHandler syncWriteMovementHandler;

    private final Queue<MotionTaskImpl> queue = new LinkedBlockingQueue<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running;

    private final Map<String, BulkPositionSpeedCommand> cachedCommands = new HashMap<>();

    @PostConstruct
    public void startListener() {
        running = true;
        executor.execute(() -> {
            LOG.info("Starting motion queue");
            while(running && !Thread.currentThread().isInterrupted()) {
                MotionTaskImpl item = queue.poll();
                if(item != null && !item.isCancelled()) {
                    try {
                        runMotion(item, null);
                    } finally {
                        item.complete();
                    }
                } else {
                    sleepUninterruptibly(INTERVAL, MILLISECONDS);
                }
            }
            LOG.info("Motion queue has stopped");
        });
    }

    @PreDestroy
    public void stopListener() {
        LOG.debug("Shutting down motion queue");
        running = false;
        executor.shutdownNow();
    }

    @Override
    public MotionTask execute(Motion motion) {
        MotionTaskImpl motionTask = new MotionTaskImpl(motion);

        LOG.debug("Scheduling motion in the queue: {}", motion);
        queue.add(motionTask);

        return motionTask;
    }

    private void runMotion(MotionTaskImpl motionTask, KeyFrame lastExecutedKeyFrame) {
        LOG.info("Executing motion task: {}", motionTask);
        motionTask.start();

        Motion motion = motionTask.getMotion();
        KeyFrame lastKeyFrame = lastExecutedKeyFrame;
        while(motion != null) {
            lastKeyFrame = executeMotion(motion, lastKeyFrame);

            motion = null; //getNextChainedMotion(motion, !motionTask.isRunning());
        }
    }

    @Override
    public MotionTask execute(KeyFrame keyFrame) {
        String motionId = UUID.randomUUID().toString();
        return execute(new MotionImpl(motionId, Lists.newArrayList(keyFrame)));
    }

    private KeyFrame executeMotion(Motion motion, KeyFrame previousKeyFrame) {
        LOG.info("Motion: {} execution", motion.getName());
        List<KeyFrame> keyFrames = motion.getFrames();
        KeyFrame lastKeyFrame = previousKeyFrame;
        Stopwatch motionWatch = createStarted();
        for (int c = 0; c < keyFrames.size(); c++) {
            Stopwatch stopwatch = createStarted();
            LOG.debug("Executing keyFrame: {}", c);

            KeyFrame keyFrame = keyFrames.get(c);

            executeKeyFrame(motion.getName(), lastKeyFrame, keyFrame);
            lastKeyFrame = keyFrame;

            LOG.info("Finished keyFrame: {} execution in: {} ms. target time: {}", c,
                    stopwatch.elapsed(MILLISECONDS), keyFrame.getTimeInMs());
        }
        LOG.info("Finished motion: {} execution in: {} ms.", motion.getName(), motionWatch.elapsed(MILLISECONDS));
        return lastKeyFrame;
    }

//    private Motion getNextChainedMotion(Motion currentMotion, boolean exitMotion) {
//        String nextMotionId = currentMotion.getNextMotion();
//        if(exitMotion) {
//            nextMotionId = currentMotion.getExitMotion();
//        }
//        return findMotion(nextMotionId);
//    }

//    private Motion findMotion(String motionId) {
//        if (motionId != null) {
//            Optional<Motion> motion = motionManager.findMotionById(motionId);
//            if (motion.isPresent()) {
//                return motion.get();
//            } else {
//                LOG.debug("Motion: {} could not be found", motionId);
//            }
//        }
//        return null;
//
//    }

    private void executeKeyFrame(String motionId, KeyFrame previousKeyFrame, KeyFrame keyFrame) {
        long timeInMs = keyFrame.getTimeInMs();

        String previousFrameId = previousKeyFrame != null ? previousKeyFrame.getKeyFrameId() : "";
        String cacheKey = motionId + "_" + previousFrameId + "_" + keyFrame.getKeyFrameId();
        if(!cachedCommands.containsKey(cacheKey)) {
            Map<String, PositionAndSpeedCommand> commands = keyFrame.getJointTargets().stream()
                    .map(s -> new PositionAndSpeedCommand(s.getJointId(), s.getTargetPosition(), new Scale(0, 1023),
                            calculateSpeed(previousKeyFrame, s.getJointId(), s.getTargetPosition(), timeInMs), new Scale(0, 1023)))
                    .collect(Collectors.toMap(PositionAndSpeedCommand::getServoId, Function.identity()));
            cachedCommands.put(cacheKey, new BulkPositionSpeedCommand(commands));
        }

        Stopwatch stopwatch = createStarted();
        BulkPositionSpeedCommand bulkPositionSpeedCommand = cachedCommands.get(cacheKey);
        syncWriteMovementHandler.receive(bulkPositionSpeedCommand);

        long s = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        LOG.debug("Finished sending: {} in {} ms.", bulkPositionSpeedCommand, s);

        //sleep minus the time it took to write to the bus
        sleepUninterruptibly(timeInMs - stopwatch.elapsed(MILLISECONDS), MILLISECONDS);
    }

    private int calculateSpeed(KeyFrame previousKeyFrame, String servoId, int targetPosition, long timeInMs) {
        int currentPosition;
        if(previousKeyFrame != null) {
            JointTarget previousServoStep = previousKeyFrame.getJointTargets().stream().filter(s -> s.getJointId().equals(servoId)).findFirst().get();
            currentPosition = previousServoStep.getTargetPosition();
        } else {
            currentPosition = dataManager.readServoProperty(servoId, ServoProperty.POSITION);
        }


        return calculateRotations(currentPosition, targetPosition, timeInMs);
    }

    public static int calculateRotations(int currentPosition, int targetPosition, long timeInMs) {
        double unitRotationsPerSecond = (0.111 / 60);
        int delta = Math.abs(targetPosition - currentPosition);
        double rotationsNeeded = ((double)delta / 1023);
        double timeInSeconds = ((double)timeInMs / 1000);
        double rotationsPerSec = rotationsNeeded / timeInSeconds;

        int speed = (int)(rotationsPerSec / unitRotationsPerSecond);
//        speed = speed * 2;
        LOG.trace("Required speed: {}", speed);

        return speed;
    }

    private class MotionTaskImpl implements MotionTask {

        private final Motion motion;
        private final String taskId = UUID.randomUUID().toString();
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final CountDownLatch latch = new CountDownLatch(1);

        public MotionTaskImpl(Motion motion) {
            this.motion = motion;
        }

        public String getTaskId() {
            return taskId;
        }

        public Motion getMotion() {
            return this.motion;
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public boolean hasCompleted() {
            return false;
        }

        @Override
        public boolean hasStarted() {
            return running.get() && !isCancelled();
        }

        @Override
        public boolean isLoop() {
            return false;
        }

        @Override
        public int getMaxLoop() {
            return 0;
        }

        public void cancel() {
            running.set(false);
            cancelled.set(true);
        }

        public void start() {
            this.running.set(true);
        }

        private void complete() {
            latch.countDown();
        }

        public void awaitCompletion() {
            Uninterruptibles.awaitUninterruptibly(latch);
        }

        @Override
        public STATE getState() {
            return null;
        }
    }
}

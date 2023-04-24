package com.oberasoftware.trainautomation;

import com.google.common.collect.Maps;
import com.oberasoftware.iot.core.AgentControllerInformation;
import com.oberasoftware.iot.core.client.ThingClient;
import com.oberasoftware.iot.core.commands.handlers.CommandHandler;
import com.oberasoftware.iot.core.exceptions.IOTException;
import com.oberasoftware.iot.core.extensions.AutomationExtension;
import com.oberasoftware.iot.core.extensions.DiscoveryListener;
import com.oberasoftware.iot.core.model.IotThing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TrainAutomationExtension implements AutomationExtension {
    private static final Logger LOG = LoggerFactory.getLogger(TrainAutomationExtension.class);

    private static final String EXTENSION_ID = "trainAutomationExtension";
    private static final String EXTENSION_NAME = "Train Automation Plugin";
    public static final String COMMAND_CENTER_TYPE = "commandCenterType";

    private final ThingClient thingClient;

    private final AgentControllerInformation agentControllerInformation;

    private final CommandCenterFactory commandCenterFactory;

    private final TrainCommandHandler commandHandler;

    public TrainAutomationExtension(ThingClient thingClient, AgentControllerInformation agentControllerInformation, CommandCenterFactory commandCenterFactory, TrainCommandHandler commandHandler) {
        this.thingClient = thingClient;
        this.agentControllerInformation = agentControllerInformation;
        this.commandCenterFactory = commandCenterFactory;
        this.commandHandler = commandHandler;
    }

    @Override
    public String getId() {
        return EXTENSION_ID;
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    @Override
    public Map<String, String> getProperties() {
        return Maps.newHashMap();
    }

    @Override
    public CommandHandler<IotThing> getCommandHandler() {
        return commandHandler;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void activate(IotThing pluginThing) {
        var properties = pluginThing.getProperties();
        Set<String> commandCenterIds = properties.keySet().stream().filter(k -> k.startsWith("cc-")).map(properties::get).collect(Collectors.toSet());
        LOG.info("Activating command centers: {}", commandCenterIds);
        commandCenterIds.forEach(cc -> {
            try {
                var oThing = thingClient.getThing(agentControllerInformation.getControllerId(), cc);
                oThing.ifPresent(this::activateCommandCenter);
            } catch (IOTException e) {
                LOG.error("Could not retrieve thing information for CommandCenter: " + cc, e);
            }
        });
        LOG.info("Finished activating command centers");
    }

    private void activateCommandCenter(IotThing commandCenter) {
        LOG.info("Activating command center connectivity: {}", commandCenter);
        var commandCenterType = commandCenter.getProperties().get(COMMAND_CENTER_TYPE);
        var oCommandCenter = commandCenterFactory.getCommandCenter(commandCenterType);

        oCommandCenter.ifPresentOrElse(c -> {
            LOG.info("Loading command center: {} for {}", commandCenterType, commandCenter);
            c.connect(commandCenter);
        }, () -> {
            LOG.error("Could not find the command center for: {} for thing: {}", commandCenterType, commandCenter);
        });
    }

    @Override
    public void discoverThings(DiscoveryListener listener) {

    }
}

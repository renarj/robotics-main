package com.oberasoftware.home.agent.core.handlers;

import com.oberasoftware.base.event.Event;
import com.oberasoftware.base.event.EventHandler;
import com.oberasoftware.base.event.EventSubscribe;
import com.oberasoftware.base.event.impl.LocalEventBus;
import com.oberasoftware.iot.core.client.AgentClient;
import com.oberasoftware.iot.core.commands.ThingCommand;
import com.oberasoftware.iot.core.commands.handlers.ThingCommandHandler;
import com.oberasoftware.iot.core.events.impl.ItemCommandEvent;
import com.oberasoftware.iot.core.exceptions.IOTException;
import com.oberasoftware.iot.core.extensions.ExtensionManager;
import com.oberasoftware.iot.core.model.IotThing;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author renarj
 */
@Component
public class ItemCommandEventHandler implements EventHandler {
    private static final Logger LOG = getLogger(ItemCommandEventHandler.class);

    @Autowired
    private ExtensionManager extensionManager;

    @Autowired
    private AgentClient agentClient;

    @Autowired
    private LocalEventBus automationBus;

    @EventSubscribe
    public Event receive(ItemCommandEvent event) throws IOTException {
        LOG.debug("Received a device command event: {}", event);

        ThingCommand command = event.getCommand();
        LOG.debug("Looking up device details for command: {} and itemId: {}",command, command.getThingId());

        Optional<IotThing> deviceData = agentClient.getThing(command.getControllerId(), command.getThingId());
        if(deviceData.isPresent()) {
            IotThing deviceItem = deviceData.get();
            String pluginId = deviceItem.getPluginId();

            var byPluginId = extensionManager.getExtensionById(pluginId);
            var byPluginName = extensionManager.getExtensionByName(pluginId);
            var plugin = byPluginId.isPresent() ? byPluginId : byPluginName;

            plugin.ifPresent(p -> {
                ThingCommandHandler commandHandler = (ThingCommandHandler) p.getCommandHandler();

                LOG.debug("Executing command: {} on extension: {}", command, p);
                if(commandHandler != null) {
                    commandHandler.receive(deviceItem, command);
                }
            });
        } else {
            //all other item types are virtual, we manage the state of this, let's publish the item value event if applicable
//            publishItemEvents(command);

//            Optional<GroupItemImpl> groupItemImpl = homeDAO.findItem(GroupItemImpl.class, command.getItemId());
//            if(groupItemImpl.isPresent()) {
//                LOG.debug("Received a group command: {} for group: {}", event, groupItemImpl.get().getName());
//
//                //return a new event, automation bus will register this
//                return new GroupCommandImpl(command, groupItemImpl.get());
//            } else {
//                LOG.warn("Could not find deviceItem information for itemId: {}", command.getItemId());
//            }
        }

        //no follow-up event to return
        return null;
    }

//    private void publishItemEvents(ItemCommand command) {
//        if(command instanceof ItemValueCommand) {
//            ItemValueCommand valueCommand = (ItemValueCommand) command;
//
//            valueCommand.getValues().forEach((k, v) -> {
//                LOG.debug("Publishing item: {} value: {} label: {}", valueCommand.getThingId(), v, k);
//                automationBus.publish(new ItemNumericValue(valueCommand.getThingId(), v, k));
//            });
//        }
//    }
}

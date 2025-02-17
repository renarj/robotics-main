package com.oberasoftware.home.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.oberasoftware.base.event.EventBus;
import com.oberasoftware.home.rules.api.Condition;
import com.oberasoftware.home.rules.api.Operator;
import com.oberasoftware.home.rules.api.general.Rule;
import com.oberasoftware.home.rules.api.general.SwitchItem;
import com.oberasoftware.home.rules.api.logic.CompareCondition;
import com.oberasoftware.home.rules.api.logic.IfStatement;
import com.oberasoftware.home.rules.api.logic.IfBranch;
import com.oberasoftware.home.rules.api.trigger.ThingTrigger;
import com.oberasoftware.home.rules.api.trigger.Trigger;
import com.oberasoftware.home.rules.api.values.ThingAttributeValue;
import com.oberasoftware.home.rules.api.values.StaticValue;
import com.oberasoftware.home.rules.test.MockStateManager;
import com.oberasoftware.iot.core.commands.SwitchCommand;
import com.oberasoftware.iot.core.exceptions.IOTException;
import com.oberasoftware.iot.core.legacymodel.VALUE_TYPE;
import com.oberasoftware.iot.core.model.states.StateImpl;
import com.oberasoftware.iot.core.model.states.StateItemImpl;
import com.oberasoftware.iot.core.model.states.ValueImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.StringWriter;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Renze de Vries
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RuleConfiguration.class, TestConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RuleEngineTest {
    private static final Logger LOG = getLogger(RuleEngineTest.class);

    private static final String CONTROLLER_ID = "testController";

    private static final String MY_ITEM_ID = "ff8daadb-5a35-4e39-ad99-5eefe73a18a7";
    private static final String LUMINANCE_LABEL = "Luminance";
    private static final String SWITCHABLE_DEVICE_ID = "SwitchableDeviceId";

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private EventBus mockAutomationBus;

    @Autowired
    private MockStateManager mockStateManager;

    @Test
    public void testSerialise() throws Exception {
        Trigger trigger = new ThingTrigger(ThingTrigger.TRIGGER_TYPE.THING_STATE_CHANGE);
        Condition condition = new CompareCondition(
                new ThingAttributeValue(CONTROLLER_ID, MY_ITEM_ID, LUMINANCE_LABEL),
                Operator.SMALLER_THAN_EQUALS,
                new StaticValue(10L, VALUE_TYPE.NUMBER));

        IfBranch branch = new IfBranch(condition, newArrayList(new SwitchItem("test", SWITCHABLE_DEVICE_ID, "switch", SwitchCommand.STATE.ON)));

        String ruleId = randomUUID().toString();
        Rule rule = new Rule(ruleId, "Light after dark", new IfStatement(newArrayList(branch)), Lists.newArrayList(trigger));

        StringWriter stringWriter = new StringWriter();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(stringWriter, rule);

        String json = stringWriter.toString();

        LOG.debug("Rule JSON: {}", json);


        Rule readRule = objectMapper.readValue(json, Rule.class);

        StringWriter secondWriter = new StringWriter();
        objectMapper.writeValue(secondWriter, readRule);
        assertThat(secondWriter.toString(), is(json));
    }

    @Test
    public void testEvaluate() throws IOTException {
        Trigger trigger = new ThingTrigger(ThingTrigger.TRIGGER_TYPE.THING_STATE_CHANGE);

        Condition condition = new CompareCondition(
                new ThingAttributeValue(CONTROLLER_ID, MY_ITEM_ID, LUMINANCE_LABEL),
                Operator.SMALLER_THAN_EQUALS,
                new StaticValue(10L, VALUE_TYPE.NUMBER));

        IfBranch branch = new IfBranch(condition, newArrayList(new SwitchItem("test", SWITCHABLE_DEVICE_ID, "switch", SwitchCommand.STATE.ON)));

        String ruleId = randomUUID().toString();
        Rule rule = new Rule(ruleId, "Light after dark", new IfStatement(newArrayList(branch)), Lists.newArrayList(trigger));

        StateImpl itemState = new StateImpl(CONTROLLER_ID, MY_ITEM_ID);
        itemState.updateIfChanged(LUMINANCE_LABEL, new StateItemImpl(LUMINANCE_LABEL, new ValueImpl(VALUE_TYPE.NUMBER, 1L)));
        mockStateManager.addState(itemState);

        ruleEngine.register(rule);


        ruleEngine.evalRule(ruleId);

//        List<Event> publishedEvents = mockAutomationBus.getPublishedEvents();
//        assertThat(publishedEvents.size(), is(1));
//
//        Event event = publishedEvents.get(0);
//        assertThat(event instanceof ItemCommandEvent, is(true));
//        ItemCommandEvent switchCommand = (ItemCommandEvent) event;
//        assertThat(switchCommand.getItemId(), is(SWITCHABLE_DEVICE_ID));
//        assertThat(((SwitchCommand)switchCommand.getCommand()).getState(), is(SwitchCommand.STATE.ON));
    }

    @Test
    public void testDeviceMovement() throws  Exception {
        Trigger trigger = new ThingTrigger(ThingTrigger.TRIGGER_TYPE.THING_STATE_CHANGE);

        Condition condition = new CompareCondition(
                new ThingAttributeValue(CONTROLLER_ID, MY_ITEM_ID, "on-off"),
                Operator.EQUALS,
                new StaticValue("on", VALUE_TYPE.STRING));

        IfBranch branch = new IfBranch(condition, newArrayList(new SwitchItem("test", "LightId", "switch", SwitchCommand.STATE.ON)));
        String ruleId = randomUUID().toString();
        Rule rule = new Rule(ruleId, "Light on with movement", new IfStatement(newArrayList(branch)), Lists.newArrayList(trigger));

        StringWriter stringWriter = new StringWriter();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(stringWriter, rule);

        String json = stringWriter.toString();

        LOG.debug("Rule JSON: {}", json);



        StateImpl itemState = new StateImpl(CONTROLLER_ID, MY_ITEM_ID);
        itemState.updateIfChanged("on-off", new StateItemImpl("on-off", new ValueImpl(VALUE_TYPE.STRING, "on")));
        mockStateManager.addState(itemState);

        ruleEngine.register(rule);

        ruleEngine.evalRule(ruleId);

//        List<Event> publishedEvents = mockAutomationBus.getPublishedEvents();
//        assertThat(publishedEvents.size(), is(1));
//
//        Event event = publishedEvents.get(0);
//        assertThat(event instanceof ItemCommandEvent, is(true));
//        ItemCommandEvent switchCommand = (ItemCommandEvent) event;
//        assertThat(switchCommand.getItemId(), is("LightId"));
//        assertThat(((SwitchCommand)switchCommand.getCommand()).getState(), is(SwitchCommand.STATE.ON));

    }

}

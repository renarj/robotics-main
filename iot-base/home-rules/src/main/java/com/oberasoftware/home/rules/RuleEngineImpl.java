package com.oberasoftware.home.rules;

import com.oberasoftware.home.rules.api.Statement;
import com.oberasoftware.home.rules.api.general.Rule;
import com.oberasoftware.home.rules.evaluators.EvalException;
import com.oberasoftware.home.rules.evaluators.EvaluatorFactory;
import com.oberasoftware.home.rules.evaluators.blocks.BlockEvaluator;
import com.oberasoftware.home.rules.triggers.TriggerProcessor;
import com.oberasoftware.iot.core.exceptions.IOTException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Renze de Vries
 */
@Component
public class RuleEngineImpl implements RuleEngine {
    private static final Logger LOG = getLogger(RuleEngineImpl.class);

    @Autowired
    private EvaluatorFactory evaluatorFactory;

    @Autowired
    private List<TriggerProcessor> triggerProcessors;

    private final List<Rule> rules = new CopyOnWriteArrayList<>();

    @Override
    public void register(Rule rule) throws IOTException {
        checkNotNull(rule);

        if(rules.stream().anyMatch(r -> r.getId().equals(rule.getId()))) {
            LOG.info("Updating existing rule, removing old rule: {}", rule.getId());
            removeRule(rule.getId());
        }

        LOG.info("Registering rule: {}", rule);
        processTriggers(rule);

        this.rules.add(rule);
    }

    private void processTriggers(Rule rule) {
        rule.getTriggers().forEach(t -> {
            LOG.debug("Processing trigger: {}", t);
            triggerProcessors.forEach(p -> p.register(t, rule));
        });
    }

    @Override
    public void evalRule(String id) {
        checkNotNull(id);

        Optional<Rule> optionalRule = rules.stream().filter(r -> r.getId().equals(id)).findFirst();
        if(optionalRule.isPresent()) {
            eval(optionalRule.get());
        } else {
            LOG.warn("Could not evaluate rule: {} was not found", id);
        }
    }

    @Override
    public void removeRule(String id) {
        Optional<Rule> rule = this.rules.stream().filter(r -> r.getId().equals(id)).findFirst();
        if(rule.isPresent()) {
            LOG.debug("Removing rule: {}", rule);

            removeTriggers(rule.get());
            this.rules.remove(rule.get());
        }
    }

    @Override
    public void onStarted() {
        triggerProcessors.forEach(TriggerProcessor::onStarted);
    }

    @Override
    public void onStopping() {
        triggerProcessors.forEach(TriggerProcessor::onStopping);
    }

    private void removeTriggers(Rule rule) {
        rule.getTriggers().forEach(t -> {
            LOG.debug("Unregistering trigger: {}", t);

            triggerProcessors.forEach(p -> p.remove(t, rule));
        });
    }

    private void eval(Rule rule) {
        LOG.info("Evaluating rule: {}", rule);
        rule.getBlocks().forEach(b -> {
            BlockEvaluator<Statement> e = evaluatorFactory.getEvaluator(b);
            try {
                boolean eval = e.eval(b);

                LOG.info("Rule: {} was evaluated: {}", rule, eval);
            } catch(EvalException ex) {
                LOG.warn("Rule could not be evaluated: {}", ex.getMessage());
            }
        });

    }
}

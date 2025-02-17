package com.oberasoftware.iot.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oberasoftware.iot.core.client.StateClient;
import com.oberasoftware.iot.core.exceptions.IOTException;
import com.oberasoftware.iot.core.model.states.State;
import com.oberasoftware.iot.core.model.states.StateImpl;
import com.oberasoftware.iot.core.util.HttpUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DefaultStateClient implements StateClient {
    private static final Logger LOG = getLogger( DefaultStateClient.class );

    private HttpClient client;

    @Value("${state-svc.baseUrl:}")
    private String baseUrl;

    @Value("${state-svc.apiToken:}")
    private String apiToken;

    @PostConstruct
    public void postConstruct() {
        this.client = HttpUtils.createClient(false);
    }

    @Override
    public void configure(String baseUrl, String apiToken) {
        this.baseUrl = baseUrl;
        this.apiToken = apiToken;
    }

    @Override
    public Optional<State> getState(String controllerId, String thingId) throws IOTException {
        var request = HttpRequest.newBuilder()
                .uri(UriBuilder.create(baseUrl).resource("state").resource("controllers", controllerId).resource("things", thingId).build())
                .build();
        LOG.debug("Doing HTTP Request: {}", request);
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() == HttpStatus.OK.value()) {
                var body = response.body();
                if(body.contains("stateItems")) {
                    ObjectMapper mapper = new ObjectMapper();
                    StateImpl state = mapper.readValue(body, StateImpl.class);

                    LOG.debug("Found State: {}", state);
                    return Optional.of(state);
                } else {
                    LOG.debug("No state was found for controller: {} thing: {}", controllerId, thingId);
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("", e);
            throw new IOTException("Unable to request State from service", e);
        }

    }
}

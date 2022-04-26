package com.oberasoftware.robo.maximus;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.oberasoftware.robo.dynamixel.SerialDynamixelConnector;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.oberasoftware.robo.dynamixel.protocolv2.DynamixelV2CommandPacket.bb2hex;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Primary
public class TeensyProxySerialConnector extends SerialDynamixelConnector {
    private static final Logger LOG = getLogger(TeensyProxySerialConnector.class);

    private static final String DXL_MSG = "{\"command\":\"%s\",\"wait\":\"%s\",\"dxldata\":\"%s\"}";

    private static final String JSON_HEAD = "{\"command\":\"%s\"";
    private static final String JSON_PARAM = ",\"%s\":\"%s\"";

    public TeensyProxySerialConnector() {
        super();

        baudRate = 57600;
        responseDelayTime = 1000;
    }

    public byte[] sendAndReceiveCommand(String command, Map<String, String> parameters, boolean wait) {
        StringBuilder builder = new StringBuilder(String.format(JSON_HEAD, command));
        parameters.forEach((k, v) -> {
            builder.append(String.format(JSON_PARAM, k, v));
        });
        builder.append("}");

        return sendInternal(builder.toString(), wait);
    }

    @Override
    public void sendNoReceive(byte[] bytes) {
        send(bytes, false);
    }

    @Override
    public synchronized byte[] sendAndReceive(byte[] bytes) {
        return send(bytes, true);
    }

    protected byte[] send(byte[] bytes, boolean wait) {
        LOG.debug("Sending Dynamixel package to Teensy: {}", bb2hex(bytes));

        Map<String, String> params = ImmutableMap.<String, String>builder().put("wait", Boolean.toString(wait)).put("dxldata", bb2hex(bytes, false)).build();
        return sendAndReceiveCommand("dynamixel", params, wait);
    }

    private byte[] sendInternal(String msg, boolean wait) {
        LOG.debug("Sending message to Teensy: {}", msg);
        Stopwatch w = Stopwatch.createStarted();
        byte[] recvd = super.send(msg.getBytes(Charset.defaultCharset()), wait);
        if(recvd.length > 0 && wait) {
            LOG.debug("Received: {} on request: {} in {} ms.", new String(recvd), msg, w.elapsed(TimeUnit.MILLISECONDS));

            JSONObject jo = new JSONObject(new String(recvd));
            String format = jo.getString("format");

            if("hex".equalsIgnoreCase(format)) {
                String hexData = jo.getString("feedback");
                if(hexData.length() > 0) {
                    String[] hs = hexData.split(" ");
                    byte[] converted = new byte[hs.length];
                    for (int i = 0; i < hs.length; i++) {
                        String element = hs[i];
                        if (element.trim().length() > 0) {
                            converted[i] = (byte) Integer.parseInt(hs[i], 16);
                        }
                    }

                    return converted;
                }
            } else if ("json".equalsIgnoreCase(format)){
                return jo.getJSONObject("feedback").toString().getBytes();
            }
        } else if(wait) {
            LOG.info("Received nothing?: {} on message request: {}", recvd, msg);
        }

        return new byte[]{};
    }
}

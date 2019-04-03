var stompClient = null;

function connect() {
    console.log("Connecting to websocket");
    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function(frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/joints', function(frame){
            handleStateUpdate(JSON.parse(frame.body));
        });
    });
    stompClient.debug = null
}

function handleStateUpdate(state) {
    $("#degrees-" + state.id).val(state.degrees);
    $("#position-" + state.id).val(state.position);
    $("#slider-" + state.id).slider('setValue', state.degrees);
}

function renderRobots() {
    $.get("/humanoid/", function(data) {
        console.log("Humanoid Robots: " + JSON.stringify(data));

        $.each(data, function(i, robot) {
            renderRobot(robot);
            renderJointPosition(robot);
        })
    });
}

function renderJointPosition(robot) {
    $.get("/humanoid/robot/" + robot.name + "/joints", function(data) {
        console.log("Joint Data: " + JSON.stringify(data));

        $.each(data, function(i, joint) {
            console.log("Joint data for: " + joint.id);

            $("#degrees-" + joint.id).val(joint.degrees);
            $("#position-" + joint.id).val(joint.position);
            $("#slider-" + joint.id).slider('setValue', joint.degrees);
        });
    });
}

function renderRobot(robot) {
    console.log("Rendering robot with name: " + robot.name);

    $.each(robot.chainSets, function(i, chain) {
        renderChain(chain);
    })
}

function renderChain(chain) {
    console.log("Rendering chain: " + chain.name);

    var chainId = "chain-" + chain.name;
    var data = {
        "name": chain.name,
        "id" : chainId
    };

    var rendered = renderTemplate("chain", data);
    $("#chainContainer").append(rendered);

    $.each(chain.jointChains, function(i, jointChain) {
        renderJointChain(chainId, jointChain);
    });
}

function renderJointChain(parentChainId, jointChain) {
    console.log("Rendering jointChain: " + jointChain.name)

    var jointChainId = "jointChain-" + jointChain.name;
    var data = {
        "name": jointChain.name,
        "id" : jointChainId
    };

    var rendered = renderTemplate("jointChain", data);
    $("#" + parentChainId).append(rendered);

    $.each(jointChain.joints, function(i, joint) {
        renderJoint(jointChainId, joint);
    })
}

function renderJoint(jointChainId, joint) {
    console.log("Rendering joint: " + joint.name);

    var data = {
        "name": joint.name,
        "id" : joint.id,
        "sliderId" : "slider-" + joint.id
    };

    var rendered = renderTemplate("joint", data);
    $("#" + jointChainId).append(rendered);

    var slider = $("#slider-" + joint.id);
    slider.slider();

    slider.on("slideStop", handleSlideStop);

    $("#tEnable-" + joint.id).click(function (e) {
        e.preventDefault();

        var jointId = this.getAttribute('jointid');

        console.log("We had torgue enable on joint: " + jointId);
        $.ajax({
            url: "/servos/enable/" + jointId + "/torgue",
            type: "POST",
            contentType: "application/json; charset=utf-8",
            success: function (data) {
                console.log("Enable torgue for servo successfully");
            }
        });

    });

    $("#tDisable-" + joint.id).click(function (e) {
        e.preventDefault();

        var jointId = this.getAttribute('jointid');
        console.log("We had torgue disable on joint: " + jointId);
        $.ajax({
            url: "/servos/disable/" + jointId + "/torgue",
            type: "POST",
            contentType: "application/json; charset=utf-8",
            success: function (data) {
                console.log("Disable torgue for servo successfully");
            }
        });
    });
}

function handleSlideStop(slideEvt) {
    var val = slideEvt.value;
    var jointId = this.getAttribute('jointId');

    setServoProperty(jointId, "position", val);
}

function setServoProperty(servoId, property, value) {
    $.ajax({url: "/servos/set/" + servoId + "/" + property + "/" + value, type: "POST", contentType: "application/json; charset=utf-8", success: function(data) {
            console.log("Set joint: " + servoId + " " + property + " to value: " + value + " successfully");
        }});
}

function renderTemplate(templateName, data) {
    var template = $('#' + templateName).html();
    Mustache.parse(template);
    return Mustache.render(template, data);
}

$(document).ready(function() {
    renderRobots();

    connect();
});
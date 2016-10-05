var stompClient = null;

function connect() {
    console.log("Connecting to websocket");
    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function(frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/state', function(frame){
            handleStateUpdate(JSON.parse(frame.body));
        });
    });
    stompClient.debug = null
}

function handleStateUpdate(state) {
    var positionSlider = $("#position" + state.servoId);
    var positionText = $("#textposition" + state.servoId);
    if(positionSlider.length) {
        if(positionSlider[0].getAttribute("changing") != "true") {
            var position = positionSlider.slider('getValue');
            if(position != state.position) {
                positionSlider.slider('setValue', state.position);
            }
        }

        positionText.val(state.position);
    }
}

function loadHandlers() {
    var positionSliders = $("input[slider=position]");
    // positionSliders.each(function(index){
    //     $(this).slider();
    //     $(this).on("slide", handleSlideEvent);
    // });
    positionSliders.each(function(index){
        $(this).slider();
        $(this).on("slideStart", handleSlideStart);
    });
    positionSliders.each(function(index){
        $(this).slider();
        $(this).on("slideStop", handleSlideStop);
    });

    $("button.torgueEnable").click(function (e) {
        e.preventDefault();

        var servoId = this.getAttribute('servoId');
        $.ajax({url: "/servos/enable/" + servoId + "/torgue", type: "POST", contentType: "application/json; charset=utf-8", success: function(data) {
            console.log("Enable torgue for servo successfully");
        }});

    });

    $("button.torgueDisable").click(function (e) {
        e.preventDefault();

        var servoId = this.getAttribute('servoId');
        $.ajax({url: "/servos/disable/" + servoId + "/torgue", type: "POST", contentType: "application/json; charset=utf-8", success: function(data) {
            console.log("Disable torgue for servo successfully");
        }});
    });

    $("button.setSpeed").click(function (e) {
        e.preventDefault();

        var servoId = this.getAttribute('servoId');
        var speed = $("#textspeed" + servoId).val();

        setServoProperty(servoId, "speed", speed);
    });

    $("button.setTorgue").click(function (e) {
        e.preventDefault();

        var servoId = this.getAttribute('servoId');
        var torgue = $("#texttorgue" + servoId).val();

        setServoProperty(servoId, "torgue", torgue);
    });

    $("button.setPosition").click(function (e) {
        e.preventDefault();

        var servoId = this.getAttribute('servoId');
        var position = $("#textposition" + servoId).val();

        setServoProperty(servoId, "position", position);
    });

    $("button.enableAllTorgue").click(function (e) {
        e.preventDefault();
        $.ajax({url: "/servos/enable/torgue", type: "POST", contentType: "application/json; charset=utf-8", success: function(data) {
            console.log("Enabled torgue for servos successfully");
        }});
    });

    $("button.disableAllTorgue").click(function (e) {
        e.preventDefault();
        $.ajax({url: "/servos/disable/torgue", type: "POST", contentType: "application/json; charset=utf-8", success: function(data) {
            console.log("Disabled torgue for servos successfully");
        }});
    });

    $("button.saveKeyFrame").click(function (e) {
        e.preventDefault();
        $.ajax({url: "/motions/keyframe", type: "POST", contentType: "application/json; charset=utf-8", success: function(data) {
            // console.log("Received keyframe: " + JSON.stringify(data));

            var ms = $('#motionscript');
            var current = ms.val();
            ms.val(current + JSON.stringify(data));
        }});
    });

    $("button.executeMotion").click(function (e) {
        e.preventDefault();

        var motionId = $("#motion").val();

        $.ajax({url: "/motions/run/" + motionId, type: "POST", contentType: "application/json; charset=utf-8", success: function(data) {
            console.log("Motion execution: " + motionId + " was triggered");
        }});
    });
}

// function handleSlideEvent(slideEvt) {
//     var val = slideEvt.value;
//     var servoId = this.getAttribute('servoId');
//
//     // console.log("Slide event: " + val);
// }

function handleSlideStart(slideEvt) {
    // var val = slideEvt.value;
    var servoId = this.getAttribute('servoId');
    this.setAttribute("changing", "true");
}

function handleSlideStop(slideEvt) {
    var val = slideEvt.value;
    var servoId = this.getAttribute('servoId');
    var speed = $("#textspeed" + servoId).val();

    setServoProperty(servoId, "speed", speed);
    setServoProperty(servoId, "position", val);

    this.setAttribute("changing", "false");
}

function setServoProperty(servoId, property, value) {
    $.ajax({url: "/servos/set/" + servoId + "/" + property + "/" + value, type: "POST", contentType: "application/json; charset=utf-8", success: function(data) {
        console.log("Set servo: " + servoId + " " + property + " to value: " + value + " successfully");
    }});
}

$(document).ready(function() {
    loadHandlers();
    // connect();
});
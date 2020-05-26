

const CallMe = function(cx) {
};

CallMe.prototype = new IdempotentHandler();
CallMe.prototype.constructor = CallMe;

const Repeater = function(cx) {
}

Repeater._methods = function() {
    return ['callMe'];
};

const ContainerRepeater = function() {
}

ContainerRepeater.prototype.callMe = function(cx, callback) {
    cx.log("hello, world");
}





const CallMe = function(cx) {
};

CallMe.prototype = new IdempotentHandler();
CallMe.prototype.constructor = CallMe;

const Repeater = function(cx) {
}

Repeater.prototype._areYouA = function(_cxt, ty) {
	if (_cxt.isTruthy(ty == 'Repeater')) {
	  return true;
	} else 
	  return false;
  }
  
Repeater.prototype._areYouA.nfargs = function() { return 1; }
  
Repeater._methods = function() {
    return ['callMe'];
};

const ContainerRepeater = function() {
}

ContainerRepeater.prototype.callMe = function(cx, callback) {
    return Send.eval(cx, callback, "call", []);
}



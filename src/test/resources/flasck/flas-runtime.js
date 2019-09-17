FLContext = function(env) {
}

FLContext.prototype.closure = function() {
	return new FLClosure();
}

FLContext.prototype.full = function(obj) {
	return obj;
}

FLClosure = function() {
}

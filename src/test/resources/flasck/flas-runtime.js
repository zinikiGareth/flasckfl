FLClosure = function(fn, args) {
	this.fn = fn;
	args.splice(0,0, null);
	this.args = args;
}

FLClosure.prototype.eval = function(_cxt) {
	this.args[0] = _cxt;
	this.val = this.fn.apply(null, this.args);
	return this.val;
}

FLClosure.prototype.toString = function() {
	return "FLClosure[]";
}

if (typeof(require) !== 'undefined') {
	const FLClosure = require('./closure');
}

var FLContext = function(env) {
}

FLContext.prototype.closure = function(fn, ...args) {
	return new FLClosure(fn, args);
}

FLContext.prototype.head = function(obj) {
	if (obj instanceof FLClosure)
		obj = obj.eval(this);
	return obj;
}

FLContext.prototype.full = function(obj) {
	if (obj instanceof FLClosure)
		obj = obj.eval(this);
	return obj;
}

FLContext.prototype.isA = function(val, ty) {
	switch (ty) {
	case 'Number':
		return typeof(val) == 'number';
	case 'String':
		return typeof(val) == 'string';
	case 'Nil':
		return Array.isArray(val) && val.length == 0;
	case 'Cons':
		return Array.isArray(val) && val.length > 0;
	default:
		return false;
	}
}

if (typeof(module) !== 'undefined')
	module.exports = FLContext;
else
	window.FLContext = FLContext;
class _FLError extends Error {
	constructor(msg) {
    	super(msg);
    	this.name = "FLError";
	}
	
	_compareTo(other) {
		if (!other instanceof _FLError) return false;
		if (other.message != this.message) return false;
		return true;
	}
}

var FLError = function(_cxt, msg) {
	return new _FLError(msg);
}

if (typeof(module) !== 'undefined')
	module.exports = FLError;
else
	window.FLError = FLError;
Nil = function(_cxt) {
	return [];
}
FLBuiltin = function() {
}

FLBuiltin.plus = function(_cxt, a, b) {
	return a+b;
}

FLBuiltin.mul = function(_cxt, a, b) {
	return a*b;
}

if (typeof(module) !== 'undefined') {
	module.exports = FLBuiltin;
} else {
	window.FLBuiltin = FLBuiltin;
}

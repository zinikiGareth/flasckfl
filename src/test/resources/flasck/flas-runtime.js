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

FLContext.prototype.array = function(...args) {
	return args;
}

FLContext.prototype.head = function(obj) {
	if (obj instanceof FLClosure)
		obj = obj.eval(this);
	return obj;
}

FLContext.prototype.full = function(obj) {
	while (obj instanceof FLClosure)
		obj = obj.eval(this);
	return obj;
}

FLContext.prototype.isA = function(val, ty) {
	switch (ty) {
	case 'True':
		return val === true;
	case 'False':
		return val === false;
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

FLContext.prototype.compare = function(left, right) {
	if (typeof(left) === 'number' || typeof(left) === 'string') {
		return left === right;
	} else if (Array.isArray(left) && Array.isArray(right)) {
		// not good enough
		return left.length === right.length;
	} else if (left instanceof _FLError && right instanceof _FLError) {
		return left.message === right.message;
	} else
		return false;
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
const Nil = function() {
}

Nil.eval = function(_cxt) {
	return [];
}

const Cons = function() {
}

Cons.eval = function(_cxt, hd, tl) {
	return ["NotImplemented"];
}

if (typeof(module) !== 'undefined') {
	module.exports = { Nil, Cons }
} else {
	window.Nil = Nil;
	window.Cons = Cons;
}
const True = function() {
}

True.eval = function(_cxt) {
	return true;
}

const False = function() {
}

False.eval = function(_cxt) {
	return false;
}

const FLBuiltin = function() {
}

FLBuiltin.arr_length = function(_cxt, arr) {
	arr = _cxt.head(arr);
	if (!Array.isArray(arr))
		throw new FLError("not an array");
	return arr.length;
}

FLBuiltin.plus = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a+b;
}

FLBuiltin.mul = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a*b;
}

if (typeof(module) !== 'undefined') {
	module.exports = { False, True, FLBuiltin };
} else {
	window.FLBuiltin = FLBuiltin;
	window.True = True;
	window.False = False;
}

const FLClosure = function(fn, args) {
	this.fn = fn;
	args.splice(0,0, null);
	this.args = args;
}

FLClosure.prototype.eval = function(_cxt) {
	this.args[0] = _cxt;
	this.val = this.fn.apply(null, this.args);
	return this.val;
}

FLClosure.prototype.apply = function(_, args) {
	const asfn = this.eval(args[0]);
	return asfn.apply(null, args);
}

FLClosure.prototype.toString = function() {
	return "FLClosure[]";
}


const FLCurry = function(fn, reqd, xcs) {
	this.fn = fn;
	this.args = [null];
	this.missing = [];
	for (var i=1;i<=reqd;i++) {
		if (xcs[i])
			this.args.push(xcs[i]);
		else {
			this.args.push(null);
			this.missing.push(i);
		}
	}
}

// TODO: I think this trashes the current curry; instead it should do things locally and create a new curry if needed.
FLCurry.prototype.apply = function(_, args) {
	this.args[0] = args[0];
	for (var i=1;i<args.length;i++) {
		var m = this.missing.pop();
		this.args[m] = args[i];
	}
	if (this.missing.length == 0) {
		return this.fn.apply(null, this.args);
	} else {
		return this;
	}
}

FLCurry.prototype.toString = function() {
	return "FLCurry[" + this.reqd + "]";
}



const FLContext = function(env) {
}

FLContext.prototype.closure = function(fn, ...args) {
	return new FLClosure(fn, args);
}

FLContext.prototype.curry = function(reqd, fn, ...args) {
	var xcs = {};
	for (var i=0;i<args.length;i++) {
		xcs[i+1] = args[i];
	}
	return new FLCurry(fn, reqd, xcs);
}

FLContext.prototype.xcurry = function(reqd, ...args) {
	var fn;
	var xcs = {};
	for (var i=0;i<args.length;i+=2) {
		if (args[i] == 0)
			fn = args[i+1];
		else
			xcs[args[i]] = args[i+1];
	}
	return new FLCurry(fn, reqd, xcs);
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

FLContext.prototype.isTruthy = function(val) {
	val = this.full(val);
	return !!val;
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
	} else if (left._compare) {
		return left._compare(right);
	} else
		return left == right;
}

FLContext.prototype.field = function(obj, field) {
	obj = this.full(obj);
	if (field == "head" && Array.isArray(obj) && obj.length > 0)
		return obj[0];
	else if (field == "tail" && Array.isArray(obj) && obj.length > 0)
		return obj.slice(1);
	else {
// TODO: this probably involves backing documents ...
		return obj[field];
	}
}


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

const FLError = {
}
FLError.eval = function(_cxt, msg) {
	return new _FLError(msg);
}


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

FLBuiltin.minus = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a-b;
}

FLBuiltin.mul = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a*b;
}

FLBuiltin.div = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a/b;
}

FLBuiltin.isEqual = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return _cxt.compare(a,b);
}


const Debug = function() {
}
Debug.eval = function(_cxt, msg) {
	const d = new Debug();
	d.msg = msg;
	return d;
}
Debug.prototype._compare = function() {
	return true;
}

const Send = function() {
}
Send.eval = function(_cxt, obj, meth, args) {
	const s = new Send();
	s.obj = obj;
	s.meth = meth;
	s.args = args;
	return s;
}


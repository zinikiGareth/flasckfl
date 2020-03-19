const JSEnv = function() {
    this.logger = console;
}



const ContractStore = function(_cxt) {
}

ContractStore.prototype.record = function(_cxt, name, impl) {
    this[name] = impl;
}

ContractStore.prototype.contractFor = function(_cxt, name) {
    const ret = this[name];
    if (!ret)
        throw new Error("There is no contract for " + name);
    return ret;
}


const FLClosure = function(obj, fn, args) {
	/* istanbul ignore if */
	if (!fn)
		throw new Error("must define a function");
	this.obj = obj;
	this.fn = fn;
	args.splice(0,0, null);
	this.args = args;
}

FLClosure.prototype.eval = function(_cxt) {
	this.args[0] = _cxt;
	this.obj = _cxt.full(this.obj);
	var cnt = this.fn.nfargs();
	this.val = this.fn.apply(this.obj, this.args.slice(0, cnt+1)); // +1 for cxt
	// handle the case where there are arguments left over
	if (cnt+1 < this.args.length) {
		this.val = new FLClosure(this.obj, this.val, this.args.slice(cnt+1));
	}
	return this.val;
}

FLClosure.prototype.apply = function(_, args) {
	const asfn = this.eval(args[0]);
	return asfn.apply(null, args);
}

FLClosure.prototype.nfargs = function() { return 0; }

FLClosure.prototype.toString = function() {
	return "FLClosure[]";
}


const FLCurry = function(obj, fn, reqd, xcs) {
	this.obj = obj;
	this.fn = fn;
	this.args = [null];
	this.reqd = reqd;
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
		return this.fn.apply(this.obj, this.args);
	} else {
		return this;
	}
}

FLCurry.prototype.nfargs = function() {
	return this.reqd;
}

FLCurry.prototype.toString = function() {
	return "FLCurry[" + this.reqd + "]";
}



const FLMakeSend = function(meth, obj, nargs, /* optional */ args) {
	this.meth = meth;
	this.obj = obj;
	this.nargs = nargs;
	if (args)
		this.current = args;
	else
		this.current = [];
}

FLMakeSend.prototype.apply = function(cx, args) {
	var all = this.current.slice();
	for (var i=1;i<args.length;i++)
		all.push(args[i]);
	if (all.length == this.nargs) {
		return Send.eval(cx, this.obj, this.meth, all);
	} else {
		return new FLMakeSend(this.meth, this.obj, this.nargs, all);
	}
}

FLMakeSend.prototype.nfargs = function() { return this.nargs; }

FLMakeSend.prototype.toString = function() {
	return "MakeSend[" + this.nargs + "]";
}



const FLContext = function(env) {
	this.env = env;
}

FLContext.prototype.log = function(...args) {
	this.env.logger.log.apply(this.env.logger, args);
}

FLContext.prototype.closure = function(fn, ...args) {
	return new FLClosure(null, fn, args);
}

FLContext.prototype.oclosure = function(fn, obj, ...args) {
	return new FLClosure(obj, fn, args);
}

FLContext.prototype.curry = function(reqd, fn, ...args) {
	var xcs = {};
	for (var i=0;i<args.length;i++) {
		xcs[i+1] = args[i];
	}
	return new FLCurry(null, fn, reqd, xcs);
}

FLContext.prototype.ocurry = function(reqd, fn, obj, ...args) {
	var xcs = {};
	for (var i=0;i<args.length;i++) {
		xcs[i+1] = args[i];
	}
	return new FLCurry(obj, fn, reqd, xcs);
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
	return new FLCurry(null, fn, reqd, xcs);
}

FLContext.prototype.array = function(...args) {
	return args;
}

FLContext.prototype.makeTuple = function(...args) {
	return Tuple.eval(this, args);
}

FLContext.prototype.tupleMember = function(tuple, which) {
	tuple = this.head(tuple);
	if (!tuple instanceof Tuple)
		throw "not a tuple: " + tuple;
	return tuple.args[which];
}

FLContext.prototype.error = function(msg) {
	return FLError.eval(this, msg);
}

FLContext.prototype.mksend = function(meth, obj, cnt) {
	if (cnt == 0)
		return Send.eval(this, obj, meth, []);
	else
		return new FLMakeSend(meth, obj, cnt);
}

FLContext.prototype.mkacor = function(meth, obj, cnt) {
	if (cnt == 0)
		return this.oclosure(meth, obj);
	else
		return this.ocurry(cnt, meth, obj);
}

FLContext.prototype.fields = function() {
	return new FieldsContainer();
}

FLContext.prototype.head = function(obj) {
	while (obj instanceof FLClosure)
		obj = obj.eval(this);
	return obj;
}

FLContext.prototype.full = function(obj) {
	obj = this.head(obj);
	if (obj == null) {
		// nothing to do
	} else if (obj._full) {
		obj._full(this);
	} else if (Array.isArray(obj)) {
		for (var i=0;i<obj.length;i++)
			obj[i] = this.full(obj[i]);
	}
	return obj;
}

FLContext.prototype.isTruthy = function(val) {
	val = this.full(val);
	return !!val;
}

FLContext.prototype.isA = function(val, ty) {
	if (val instanceof Object && 'areYouA' in val) {
		return val.areYouA(ty);
	}
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
		if (left.length !== right.length)
			return false;
		for (var i=0;i<left.length;i++) {
			if (!this.compare(left[i], right[i]))
				return false;
		}
		return true;
	} else if (left instanceof FLError && right instanceof FLError) {
		return left.message === right.message;
	} else if (left._compare) {
		return left._compare(this, right);
	} else if (left.state && right.state && left.state instanceof FieldsContainer && right.state instanceof FieldsContainer) {
		return left.state._compare(this, right.state);
	} else
		return left == right;
}

FLContext.prototype.field = function(obj, field) {
	obj = this.full(obj);
	if (Array.isArray(obj)) {
		if (field == 'head') {
			if (obj.length > 0)
				return obj[0];
			else
				return this.error('head(nil)');
		} else if (field == 'tail') {
			if (obj.length > 0)
				return obj.slice(1);
			else
				return this.error('tail(nil)');
		} else
			return this.error('no function "' + field + "'");
	} else {
		// assume it's a fields document with a state object
		// This is possibly a bogus assumption
		return obj.state.get(field);
	}
}

FLContext.prototype.mockContract = function(contract) {
	return new MockContract(contract);
}

FLContext.prototype.mockAgent = function(agent) {
	return new MockAgent(agent);
}


class FLError extends Error {
	constructor(msg) {
    	super(msg);
    	this.name = "FLError";
	}
	
	_compare(cx, other) {
		if (!(other instanceof FLError)) return false;
		if (other.message != this.message) return false;
		return true;
	}
}

FLError.eval = function(_cxt, msg) {
	return new FLError(msg);
}


/* istanbul ignore next */
const Nil = function() {
}

Nil.eval = function(_cxt) {
	return [];
}

/* istanbul ignore next */
const Cons = function() {
}

Cons.eval = function(_cxt, hd, tl) {
	var cp = tl.slice(0);
	cp.splice(0, 0, hd);
	return cp;
}



/* istanbul ignore next */
const True = function() {
}

True.eval = function(_cxt) {
	return true;
}

/* istanbul ignore next */
const False = function() {
}

False.eval = function(_cxt) {
	return false;
}

/* istanbul ignore next */
const Tuple = function() {
}

Tuple.eval = function(_cxt, args) {
	const ret = new Tuple();
	ret.args = args;
	return ret;
}

/* istanbul ignore next */
const FLBuiltin = function() {
}

FLBuiltin.arr_length = function(_cxt, arr) {
	arr = _cxt.head(arr);
	if (!Array.isArray(arr))
		return _cxt.error("not an array");
	return arr.length;
}

FLBuiltin.arr_length.nfargs = function() { return 1; }

FLBuiltin.plus = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a+b;
}

FLBuiltin.plus.nfargs = function() { return 2; }

FLBuiltin.minus = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a-b;
}

FLBuiltin.minus.nfargs = function() { return 2; }

FLBuiltin.mul = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a*b;
}

FLBuiltin.mul.nfargs = function() { return 2; }

FLBuiltin.div = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a/b;
}

FLBuiltin.div.nfargs = function() { return 2; }

FLBuiltin.concat = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a + b;
}

FLBuiltin.concat.nfargs = function() { return 2; }

FLBuiltin.strlen = function(_cxt, str) {
	str = _cxt.head(str);
	if (typeof(str) != "string")
		return _cxt.error("not a string");
	return str.length;
}

FLBuiltin.strlen.nfargs = function() { return 1; }

FLBuiltin.isEqual = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return _cxt.compare(a,b);
}

FLBuiltin.isEqual.nfargs = function() { return 2; }



const Debug = function() {
}
Debug.eval = function(_cxt, msg) {
	const d = new Debug();
	d.msg = msg;
	return d;
}
Debug.prototype._compare = function(cx, other) {
	if (other instanceof Debug) {
		return other.msg == this.msg;
	} else
		return false;
}
Debug.prototype.dispatch = function(cx) {
	cx.log(this.msg);
	return null;
}
Debug.prototype.toString = function() {
	return "Debug[" + this.msg + "]";
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
Send.prototype._full = function(cx) {
	this.obj = cx.full(this.obj);
	this.meth = cx.full(this.meth);
	this.args = cx.full(this.args);
}
Send.prototype._compare = function(cx, other) {
	if (other instanceof Send) {
		return cx.compare(this.obj, other.obj) && cx.compare(this.meth, other.meth) && cx.compare(this.args, other.args);
	} else
		return false;
}
Send.prototype.dispatch = function(cx) {
	var args = this.args.slice();
	args.splice(0, 0, cx);
	if (this.obj instanceof MockContract) {
		// TODO: specifying MockContract is obviously over-precise
		// We need to cater for actual contract implementations when we have them (ie some kind of ZiWSH proxy)
		// But the code is possibly sufficiently different that we want to keep the cases separate - not sure
		this.obj.serviceMethod(cx, this.meth, this.args);
		return [];
	} else {
		// assume it is an object with declared methods
		var ret = this.obj.methods()[this.meth].apply(this.obj, args);
		return ret;
	}
}
Send.prototype.toString = function() {
	return "Send[" + "]";
}

const Assign = function() {
}
Assign.eval = function(_cxt, obj, slot, expr) {
	const s = new Assign();
	s.obj = obj;
	s.slot = slot;
	s.expr = expr;
	return s;
}
Assign.prototype._compare = function(cx, other) {
	if (other instanceof Assign) {
		return cx.compare(this.obj, other.obj) && cx.compare(this.slot, other.slot) && cx.compare(this.expr, other.expr);
	} else
		return false;
}
Assign.prototype.dispatch = function(cx) {
	this.obj.state.set(this.slot, this.expr);
	return null;
}
Assign.prototype.toString = function() {
	return "Assign[" + "]";
}


const FieldsContainer = function() {
	this.dict = {};
}

FieldsContainer.prototype.set = function(fld, val) {
	this.dict[fld] = val;
}

FieldsContainer.prototype.get = function(fld) {
	return this.dict[fld];
}

FieldsContainer.prototype._compare = function(cx, other) {
	if (Object.keys(this.dict).length != Object.keys(other.dict).length)
		return false;
	for (var k in this.dict) {
		if (!other.dict.hasOwnProperty(k))
			return false;
		else if (!cx.compare(this.dict[k], other.dict[k]))
			return false;
	}
	return true;
}

FieldsContainer.prototype.toString = function() {
	return "Fields[" + Object.keys(this.dict).length + "]";
}



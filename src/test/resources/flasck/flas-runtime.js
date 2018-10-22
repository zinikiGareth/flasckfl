var closureCount = 0;

function FLClosure(obj, fn, args) {
//	console.log("new closure for ", fn);
	this._closure = ++closureCount;
	this.obj = obj;
	this.fn = fn;
	this.args = args;
}

FLClosure.prototype.toString = function() {
	return "Closure[" + this._closure +"]";
}

function FLRequireObject(refc) {
	this.refc = refc;
	console.log("Will need to recover " + refc.id + " of " + refc._ctor);
}

function FLEval() {
}

FLEval.head = function(x) {
//	console.log("head(" + x + ")");
	try {
		while (true) {
			if (x instanceof FLClosure) {
	//			console.log("evaluating " + x.fn);
				if (x.hasOwnProperty('value'))
					return x.value;
				var clos = x;
				if (x.fn instanceof FLClosure)
				  x.fn = FLEval.head(x.fn);
				if (!x.fn || !x.fn.apply)
				  return x.fn;
				x = clos.value = x.fn.apply(x.obj, x.args);
	//			console.log("head saw " + x);
			} else if (typeof x === "function" && x.length == 0 && !x.iscurry) {
				x = x();
			} else
				break;
		}
	} catch (ex) {
		return new FLError(ex);
	}
	return x;
}

FLEval.full = function(x) {
	// head evaluate me
	x = FLEval.head(x);
//	console.log("full(" + x + ")");
	// fully evaluate all my props
	if (x !== null && typeof x === 'object' && x['_ctor']) {
//		console.log("ctor = " + x['_ctor']);
		for (var p in x) {
			if (p[0] !== '_' && x.hasOwnProperty(p)) {
//				console.log("fully evaluating " + p, x[p], x[p].constructor == Array);
				if (!x[p])
					continue;
				else if (x[p] instanceof Array) {
					var y = x[p];
					for (var i=0;i<y.length;i++) {
					    if (y[i] instanceof FLClosure)
					    	y[i] = FLEval.full(y[i]);
					}
				} else
					x[p] = FLEval.full(x[p]);
			}
		}
	}
	return x;
}

FLEval.closure = function() {
	var args = [];
	for (var i=1;i<arguments.length;i++)
		args[i-1] = arguments[i];
	return new FLClosure(null, arguments[0], args);
}

FLEval.oclosure = function() {
	var args = [];
	for (var i=2;i<arguments.length;i++)
		args[i-2] = arguments[i];
	return new FLClosure(arguments[0], arguments[1], args);
}

FLEval.field = function(from, fieldName) {
//	console.log("get field " + fieldName +" from ", from);
	from = FLEval.head(from);
	if (from === null || from === undefined)
		return null;
	return from[fieldName];
}

FLEval.method = function(obj, methodName) {
//	console.log("call method", methodName, "on", obj, "with", arguments.length-2, "arguments");
	var method = obj[methodName];
	var args = [];
	for (var i=2;i<arguments.length;i++)
		args[i-2] = arguments[i];
	return new FLClosure(obj, method, args);
}

FLEval.tuple = function() { // need to use arguments because it's varargs
  "use strict";
  return new _Tuple(arguments); // defined in builtin
}

FLEval.octor = function(obj, meth) {
  "use strict";
  var args = [];
  for (var i=2;i<arguments.length;i++)
	args[i-2] = arguments[i];
  if (!obj[meth])
    throw new Error(obj + " does not have a constructor method " + meth);
  return obj[meth].apply(obj, args);  
}

/* length is a reserved word in Javascript,
  so we call it _length instead, and need map it appropriately
  in code generation
 */
FLEval._length = function(list) {
    list = FLEval.head(list);
	if (list == Nil || list == NilMap)
	    return 0;
	else if (list instanceof _Cons) {
		if (list._arr)
			return list._arr.length;
			
		var tl = FLEval._length(list.tail);
		if (tl instanceof FLError)
			return tl;
	    return 1 + tl;
	} else if (list instanceof _Assoc) {
		if (list._map)
			return Object.keys(list._map).length;
		var len = FLEval._length(_Assoc.rest);
		if (tl instanceof FLError)
			return tl;
	    return 1 + tl;
	} else {
		debugger;
		return new FLError("Not a valid list");
	}
}

FLEval.flattenList = function(list) {
	list = FLEval.full(list);
	if (list instanceof Array)
		return list;
	var ret = [];
	while (list && list._ctor == 'Cons') {
		ret.push(list.head);
		list = list.tail;
	}
	return ret;
}

FLEval.isA = function(obj, type) {
	if (!obj) return false;
	if (obj._ctor === type) return true;
	if (obj._special === 'contract' && obj._contract === type) return true;
	return false;
}

FLEval.flattenMap = function(obj) {
	var ret = {};
	while (obj && obj._ctor === 'Assoc') {
		ret[obj.key] = obj.value;
		obj = obj.rest;
	}
	return ret;
}

// This may or may not be valuable
// The idea behind this is to try and track where something came from when we want to save it
FLEval.fromWireService = function(addr, obj, env) {
	var ret = FLEval.fromWire(obj, env);
	if (ret instanceof Object && ret._ctor)
		ret._fromService = addr;
	return ret;
}

/* fromWire has been reworked, but toWire has not (yet).
 * The definitions are all tested in testWireFormat.html along with the corresponding data/.../....json files
 * Check there for documentation
 */

FLEval.fromWire = function(obj, env) {
	"use strict"
	if (!(obj instanceof Object))
		return obj; // it's a primitive
	if (obj._ctor) {
		// handle structs, entities, objects, etc.
		var spl = obj._ctor.split('.');
		if (spl.length == 0)
			return new FLError('no such class ' + obj._ctor);
			
		// remove the constructor field
		delete obj['_ctor'];
		
		// get the real class, not the wrapper
		var pkg = window;
		for (var i=0;i<spl.length-1;i++) {
			pkg = pkg[spl[i]];
		}
		var bn = spl[spl.length-1];
		var clz = pkg[bn];
		if (clz && clz._fromWire)
			return clz._fromWire(env, obj);
		clz = pkg['_' + bn];
		if (clz && clz instanceof Function) {
			// turn any nested reference objects into closures to get that object
			for (var k in obj) {
				if (obj.hasOwnProperty(k)) {
					if (obj[k] instanceof Object) {
						obj[k] = new FLClosure(undefined, FLEval.recoverObject, [obj[k]]);
					}
				}
			}
			
			return new clz(obj);
		}
		return new FLError('not implemented');
	} else if (obj instanceof Array) {
		return Cons.fromArray(obj);
	} else {
		// If the object has any members, return a lazy extractor
		for (var k in obj) {
			if (obj.hasOwnProperty(k))
				return Assoc.fromObject(obj); 
		}
		
		// return an empty map
		return NilMap;
	}
}

FLEval.recoverObject = function(refc) {
	// We need to do a lot of hard work to get all of this to work,
	// but the key thing is that we need the system to come to a halt
	// and go off and recover this object
	
	return new FLRequireObject(refc);
}

FLEval.toWire = function(wrapper, obj, dontLoop) {
	"use strict"
	if (obj instanceof FLClosure)
		obj = FLEval.full(obj);
	if (!(obj instanceof Object))
		return obj; // a primitive
	if (obj instanceof Array)
		throw new Error("We should not have loose arrays internally");
	if (obj._ctor === 'Nil' || obj._ctor === 'Cons') {
		if (dontLoop)
			throw new Error("Found list in a field and don't know what to do");
		var ret = [];
		while (obj && obj._ctor === 'Cons') {
			ret.push(FLEval.toWire(wrapper, obj.head, true));
			obj = obj.tail;
		}
		return ret;
	}
	if (obj._ctor === 'Croset')
		return obj.toWire();
	if (obj._ctor === 'Assoc' || obj._ctor === 'NilMap') {
		if (dontLoop)
			throw new Error("Found map in a field and don't know what to do");
		var ret = {};
		while (obj && obj._ctor === 'Assoc') {
			var val = FLEval.toWire(wrapper, obj.value, true);
			ret[obj.key] = val;
			obj = obj.rest;
		}
		return ret;
	}
	if (obj._special)
		return wrapper.convertSpecial(obj);

	// pack a shallow copy
	var ret = {};
	for (var x in obj) {
		if (obj.hasOwnProperty(x)) {
		 	if (typeof x === 'string' && x[0] === '_' && x !== '_ctor')
		 		; // delete it
		 	else
				ret[x] = FLEval.toWire(wrapper, obj[x], true);
		}
	}
	return ret;
}		

// curry a function (which can include a previous curried function)
// args are:
//   the function - a javascript function
//   arity - the expected number of arguments (needs type checking)
//   args - the remaining (insufficient) arguments
FLEval.curry = function() {
	"use strict";
	var self = this;
	var actual = arguments[0];
	var arity = arguments[1];
	var have = [];
	for (var i=2;i<arguments.length;i++)
		have[i-2] = arguments[i];
	
	var ret = function() {
		// When we get called, "more" arguments will be provided.  This may or may not be enough.
		// Copy the "already have" arguments and the new arguments into a single array
		var current = [];
		for (var i=0;i<have.length;i++)
			current[i] = have[i];
		for (var i=0;i<arguments.length;i++)
			current[have.length+i] = arguments[i];

		// If it's enough, call the method, otherwise reapply "curry" to the new set of arguments
		if (current.length >= arity)
			return FLEval.full(actual).apply(self, current);
		else
			return FLEval.curry.call(
				self,
				actual,
				arity,
				current
			);
	};
	
	ret.iscurry = true;
	
	return ret;
}

FLEval.isInteger = function(x) {
	return (typeof(x) === 'number' && Math.floor(x) === x);
}

FLEval.plus = function(a, b) {
	a = FLEval.head(a);
	if (a instanceof FLError)
		return a;
	b = FLEval.head(b);
	if (b instanceof FLError)
		return b;
	if (typeof(a) === 'number' && typeof(b) === 'number')
		return a+b;
	else
		return FLEval.error("plus: case not handled");
}

FLEval.minus = function(a, b) {
	a = FLEval.head(a);
	if (a instanceof FLError)
		return a;
	b = FLEval.head(b);
	if (b instanceof FLError)
		return b;
	if (typeof(a) === 'number' && typeof(b) === 'number')
		return a-b;
	else
		return FLEval.error("plus: case not handled");
}

FLEval.mul = function(a, b) {
	a = FLEval.head(a);
	if (a instanceof FLError)
		return a;
	b = FLEval.head(b);
	if (b instanceof FLError)
		return b;
	if (typeof(a) === 'number' && typeof(b) === 'number')
		return a*b;
	else
		return FLEval.error("plus: case not handled");
}

FLEval.mathNE = function(a, b) {
	a = FLEval.head(a);
	if (a instanceof FLError)
		return a;
	b = FLEval.head(b);
	if (b instanceof FLError)
		return b;
	if (typeof(a) === 'number' && typeof(b) === 'number')
		return a != b;
	else
		return FLEval.error("!=: case not handled");
}

FLEval.mathMod = function(a, b) {
	a = FLEval.head(a);
	if (a instanceof FLError)
		return a;
	b = FLEval.head(b);
	if (b instanceof FLError)
		return b;
	if (FLEval.isInteger(a) && FLEval.isInteger(b))
		return a % b;
	else
		return FLEval.error("%: case not handled");
}

FLEval.compeq = function(a, b) {
	a = FLEval.full(a);
	b = FLEval.full(b);
	return a == b;
}

FLEval.error = function(s) {
	return new FLError(s);
}

FLEval.makeEvent = function(ev) {
	if (ev._ctor) // if it's already an event we created
		return ev;
	switch (ev.type) {
	case "change": {
		switch (ev.target.type) {
		case "select-one":
			var opt = ev.target.selectedOptions[0];
			return new org.flasck.ChangeEvent(ev.target.selectedIndex, opt.id, opt.value);
		default: {
			console.log("cannot handle", ev.type, "for", ev.target.type);
			break;
		}
		}
	}
	default:
//		console.log("cannot convert event", ev.type);
		break;
	}
	return null;
}

FLEval;
/** A postbox is intended to be a mechanism for
 * delivering messages wherever they need to go,
 * local or remote
 * @param name the name of this postbox, to be used in generating unique names
 * @returns a new postbox
 */
Postbox = function(name, window) {
	"use strict";
	var self = this;
	this.name = name;
	this.recip = 0;
	this.postboxes = {};
	this.recipients = {};
	window.addEventListener("message", function(msg) { "use strict"; self.receiveMessage(msg) }, false);
	return this;
}

/** Create a new local-delivery address to be associated with a local component
 */
Postbox.prototype.newAddress = function() {
	"use strict"
	return "" + (++this.recip);
}

Postbox.prototype.unique = function(addr) {
	"use strict"
	return this.name + ":" + addr;
}

/** Declare a remote postbox
 * @param name the name of the remote postbox
 * @param onConnect a function to call when the postbox connects
 */
Postbox.prototype.remote = function(name, onConnect) {
	"use strict"
	if (this.postboxes[name] && this.postboxes[name].window) {
		setTimeout(function() { onConnect(name) }, 0);
	} else {
		this.postboxes[name] = { onConnect: onConnect };
	}
}

/** Connect a remote postbox
 * @param name the name of the remote postbox
 * @param pbox a window handle for the remote postbox
 */
Postbox.prototype.connect = function(name, atWindow) {
	"use strict"
	this.postboxes[name] = { window: atWindow };	
	atWindow.postMessage({action:'connect',from:this.name}, "*");
}

Postbox.prototype.receiveMessage = function(msg) {
	"use strict"
//	console.log("received", msg.data);
	if (!msg.data.from)
		throw new Error("Message did not have a from address");
	if (!msg.data.action)
		throw new Error("Message did not have an action");
	var from = msg.data.from;
	if (msg.data.action === "connect") {
		if (this.postboxes[from] && this.postboxes[from].onConnect) {
			this.postboxes[from].window = msg.source;
			this.postboxes[from].onConnect(from);
			delete this.postboxes[from].onConnect;
		} else {
			this.postboxes[from] = { window: msg.source };
		}
	} else if (msg.data.action === "data") {
		if (!this.postboxes[from] || !this.postboxes[from].window)
			throw new Error("Received data message before connect, should queue");
		console.log(this.name, "needs to process data message", msg.data.message, "at", msg.data.to);
		this.deliver(msg.data.to, msg.data.message);
	} else
		throw new Error("Cannot handle action " + msg.data.action);
}

/** Register a local component
 * @param address the local address to be used to find the component
 * @param comp the physical component to deliver to (service, impl or handler)
 */
Postbox.prototype.register = function(address, comp) {
	"use strict"
	this.recipients[address] = comp;
}

/** Remove a local component
 */
Postbox.prototype.remove = function(address) {
	"use strict"
	var idx = address.lastIndexOf(":");
	var pb = address.substr(0, idx);
	var addr = address.substr(idx+1);
	delete this.recipients[addr];
}

/** Deliver a message to an address
 * @param address the local or remote address to deliver to
 * @param invocation the invocation message to deliver to the address and invoke on the target component 
 */
Postbox.prototype.deliver = function(address, message) {
	"use strict"
	if (!address)
		throw new FLError("cannot deliver without a valid address");
	if (!message.from || !message.method || !message.args)
		throw new Error("invalid message - must contain from, method and args" + JSON.stringify(message));
//	console.log("deliver", message, "to", address);
	var idx = address.lastIndexOf(":");
	var pb = address.substr(0, idx);
	var addr = address.substr(idx+1);
	if (this.name !== pb) {
		var rpb = this.postboxes[pb];
		if (!rpb || !rpb.window)
			throw new FLError("I think this should now put things in a queue"); 
		rpb.window.postMessage({action:'data', from: this.name, to: address, message: message}, "*");
		return;
	}
	var recip = this.recipients[addr];
	if (!recip) {
		return new FLError("There is no registered recipient for " + address);
	}
	if (!recip.process)
		throw new FLError("There is no process method on" + recip);

	// deliver it directly to the recipient; just not yet.
	var fn = function() {
		recip.process(message);
	};
	// This is for the JSRunner case for FX testing, where setTimeout does not always appear to work
	if (typeof callJava !== 'undefined')
		callJava.callAsync({f: fn});
	else
		setTimeout(fn, 0);
}

Postbox.prototype.isLocal = function(addr) {
	var idx = addr.lastIndexOf(":");
	var pb = addr.substr(0, idx);
	return pb === this.name;	
}
// Builtin stuff; so core we couldn't do without it

function getPackagedItem(name) {
	"use strict";
	var scope = window;
	while (true) {
		var idx = name.indexOf(".");
		if (idx == -1)
			return scope[name];
		scope = scope[name.substring(0, idx)];
		name = name.substring(idx+1);
	}
}

function FLError(s) {
	this.message = s;
	console.log("FLAS Error encountered:", s);
	if (window.callJava)
		window.callJava.error(s);
}

FLError.prototype.toString = function() {
	return "ERROR: " + this.message;
}

// Lists

// Define an empty list by setting "_ctor" to "nil"
_Nil = function() {
	"use strict"
	this._ctor = 'Nil';
	return this;
}

_Nil.prototype.toString = function() {
	"use strict"
	return 'Nil';
}

Nil = new _Nil();

// Define a cons node by providing (possible closures for) head and tail and setting "_ctor" to "cons"
_Cons = function(a, l, arr) {
	"use strict"
	this._ctor = 'Cons';
	this.head = a;
	this.tail = l;
	this._arr = arr;
	return this;
}

_Cons.prototype.toString = function() {
	"use strict"
	return 'Cons';
}

Cons = function(a,b) { return new _Cons(a,b,undefined); }

Cons.fromArray = function(arr) {
	"use strict";
	if (arr.length == 0)
		return Nil;
	var ret = new _Cons(undefined, undefined, arr);
	ret.head = new FLClosure(ret, Cons._extractHead, [ret, 0]);
	ret.tail = new FLClosure(ret, Cons._extractTail, [ret, 1]);
	return ret;
}

Cons._extractHead = function(node, offset) {
	"use strict";
	return node.head = this._arr[offset];
}

Cons._extractTail = function(node, offset) {
	"use strict";
	if (offset == this._arr.length)
		return node.tail = Nil;
	else {
		var tail = new _Cons(undefined, undefined, undefined);
		tail.head = new FLClosure(this, Cons._extractHead, [tail, offset]);
		tail.tail = new FLClosure(this, Cons._extractTail, [tail, offset+1]);
		return node.tail = tail;
	} 
}

_StackPush = function(h, t) {
	"use strict";
	this._ctor = 'StackPush';
	this.head = h;
	this.tail = t;
	return this;
}

_StackPush.prototype.length = function() {
	"use strict"
	if (this.tail instanceof _Nil)
		return 1;
	return 1 + this.tail.length();
}

_StackPush.prototype.toString = function() {
	"use strict"
	return 'Stack' + this.length();
}

StackPush = function(h,t) {"use strict"; return new _StackPush(h,t);}
function _Tuple(members) {
	"use strict"
	this._ctor = 'Tuple';
	this.length = members.length;
	this.members = [];
	for (var i=0;i<this.length;i++)
		this.members[i] = members[i];
	return this;
}

_Tuple.prototype.toString = function() {
	"use strict"
	var ret = "(";
	var sep = "";
	for (var i=0;i<this.length;i++) {
		ret += sep + this.members[i];
		sep = ",";
	}
	return ret + ")";
}

Tuple = function() { return new _Tuple(arguments); }

// Assoc Lists or Maps or Hash-equivalent

_NilMap = function() {
	"use strict"
	this._ctor = 'NilMap';
	return this;
}

_NilMap.prototype.assoc = function() {
	"use strict"
	return null;
}

_NilMap.prototype.toString = function() {
	"use strict"
	return 'NilMap';
}

NilMap = new _NilMap();

_Assoc = function(k,v,r,obj) {
	"use strict"
	this._ctor = 'Assoc';
	this.key = k;
	this.value = v;
	this.rest = r;
	this._map = obj;
	return this;
}

_Assoc.prototype.toString = function() {
	"use strict"
	return 'Assoc';
}

Assoc = function(k,v,r) { return new _Assoc(k,v,r); }

Assoc.fromObject = function(obj) {
	"use strict"
	var ret = new _Assoc(undefined, undefined, undefined, obj);
	return ret;
}

assoc = function(map, key) {
	if (map == NilMap)
		return new FLError(key + " not in map");
	else if (!(map instanceof _Assoc))
		return new FLError("not a map");

	if (map._map) {
		if (map._map[key] === undefined)
			return new FLError(key + " not in map");
		else {
			// TODO: we need to spot objects and arrays and unpack them 
			return map._map[key];
		}
	}	
	debugger;
	return new FLError("not implemented")
}

// Message passing

_Send = function(target, method, args) {
	"use strict"
//	console.log("creating Send object, this = " + this);
	if (!this)
		throw "must be called with new";
	this._ctor = 'Send';
	this.target = target;
	this.method = method;
	this.args = args;
	return this;
}

Send = function(t, m, a) { return new _Send(t, m, a); }

_Assign = function(target, field, value) {
	"use strict";
	this._ctor = 'Assign';
	this.target = target;
	this.field = field;
	this.value = value;
}

Assign = function(target, field, value) { return new _Assign(target, field, value); }

_CreateCard = function(options, services) {
	"use strict"
	this._ctor = 'CreateCard';
	this.options = options;
	this.services = services;
}

_CreateCard.prototype.toString = function() {
	"use strict"
	return "CreateCard[" + "]";
}

CreateCard = function(options, services) { return new _CreateCard(options, services); }

_D3Action = function(action, args) {
	"use strict"
	this._ctor = 'D3Action';
	this.action = action;
	this.args = args;
}

D3Action = function(action, args) { return new _D3Action(action, args); }

_Debug = function(value) {
	"use strict";
	this._ctor = 'Debug';
	this.value = value;
}

Debug = function(value) { return new _Debug(value); }

_MessageWrapper = function(value, msgs) {
	"use strict";
	this._ctor = 'MessageWrapper';
	this.value = value;
	this.msgs = msgs;
}

MessageWrapper = function(value, msgs) { return new _MessageWrapper(value, msgs); }

_CrosetInsert = function(target, key) {
	"use strict"
	if (key._ctor !== 'Crokey' && key._ctor !== 'NaturalCrokey') throw new Error("Not a crokey");
	this._ctor = "CrosetInsert";
	this.target = target;
	this.key = key;
}
CrosetInsert = function(target, key) { return new _CrosetInsert(target, key); }

_CrosetReplace = function(target, key) {
	"use strict"
	if (key._ctor !== 'Crokey' && key._ctor !== 'NaturalCrokey') throw new Error("Not a crokey");
	this._ctor = "CrosetReplace";
	this.target = target;
	this.key = key;
}
CrosetReplace = function(target, key) { return new _CrosetReplace(target, key); }

_CrosetRemove = function(target, key, forReal) {
	"use strict"
	if (key._ctor !== 'Crokey' && key._ctor !== 'NaturalCrokey') throw new Error("Not a crokey");
	this._ctor = "CrosetRemove";
	this.target = target;
	this.key = key;
	this.forReal = forReal;
}
CrosetRemove = function(target, key, forReal) { return new _CrosetRemove(target, key, forReal); }

_CrosetMove = function(target, from, to) {
	"use strict"
	if (from._ctor !== 'Crokey' && from._ctor !== 'NaturalCrokey') throw new Error("Not a crokey");
	if (to._ctor !== 'Crokey' && to._ctor !== 'NaturalCrokey') throw new Error("Not a crokey");
	this._ctor = "CrosetMove";
	this.target = target;
	this.from = from;
	this.to = to;
}
CrosetMove = function(target, from, to) { return new _CrosetMove(target, from, to); }

_Card = function(explicit, loadId) {
	"use strict";
	this._ctor = 'Card';
	this.explicit = explicit;
	this.loadId = loadId;
}

Card = function(explicit, loadId) { return new _Card(explicit, loadId); }
// Static methods go on Flasck

Flasck = {};

Flasck.nextCard = 0;

Flasck.provideService = function(postbox, services, svcName, svc) {
	var addr = postbox.newAddress();
	postbox.register(addr, svc);
	svc._myAddr = services[svcName] = postbox.unique(addr);
}

Flasck.createCard = function(postbox, inside, cardInfo, services) {
	// create a "parent" view of the world
	var myAddr;
	if (cardInfo.mode === 'in_iframe') {
		postbox.connect("main", cardInfo.mainWindow);
		myAddr = cardInfo.addr;
	} else {
		// this is the thing that's supposed to handle our end of the Init contract
		var myEnd = postbox.newAddress();
		myAddr = postbox.unique(myEnd);
	
		// create an object that is the creator's handle to the card
		var handle = new FlasckHandle(postbox, myAddr);
	
		var initService = {
			process: function(message) {
	//			console.log("need to process", message);
				if (message.method === 'ready')
					this.ready(message.from, message.args[0]);
				else
					throw new Error("Cannot process " + message.method);
			},
			ready: function(from, contracts) {
				var reply = {};
				for (var ctr in contracts) {
					if (services[ctr]) {
						reply[ctr] = services[ctr];
						handle.channels[ctr] = contracts[ctr];
					}
				}
				// hack ... but we need something like this for pass-through
				for (var s in services) {
					if (!reply[s])
						reply[s] = services[s];
				}
				// end hack
	//			console.log("ah ... card is ready and wants ", contracts, " and will get ", reply);
				postbox.deliver(from, {from: myAddr, method: "services", args: [reply]});
				postbox.deliver(from, {from: myAddr, method: "state", args: [] });
				if (cardInfo.loadId)
					postbox.deliver(from, {from: myAddr, method: "loadId", args: [cardInfo.loadId] });
				if (contracts['org.flasck.Render']) {
					// it's not possible to clone a div across boundaries; only do this if we are passing locally
					// the "in_iframe" case needs to figure its own div
					var divarg = null;
					if (cardInfo.mode !== 'remote')
						divarg = inside;
					postbox.deliver(contracts['org.flasck.Render'], {from: myAddr, method: "render", args: [{into: divarg}] }); 
				}
				for (var i=0;i<handle.pending.length;i++) {
					var msg = handle.pending[i];
					var chan = handle.channels[msg.ctr];
					if (!chan)
						throw new Error("There is no channel " + msg.ctr);
					delete msg.ctr;
					handle.postbox.deliver(chan, msg);
				}
				delete handle.pending;
			}
		};
		services['org.flasck.Init'] = myAddr;
		postbox.register(myEnd, initService);
	}
	
	// now create a "child" view of the world	
	if (cardInfo.mode === 'local' || cardInfo.mode === 'in_iframe' || cardInfo.mode === 'overlay') {
		var cardClz = cardInfo.explicit;
		if (!cardClz)
			throw new Error("Must specify a valid card class object in cardInfo.explicit");
		
		// Create a wrapper around the card which is its proto-environment to link back up to the real environment
		var wrapper = new FlasckWrapper(postbox, myAddr, cardClz, inside, "card_" + (++Flasck.nextCard));
	
		// Now create the card and tell the wrapper about it
		var myCard = cardClz({ wrapper: wrapper });
	//	console.log("Creating card", myCard._ctor);
		for (var s in myCard._services) {
	//		console.log("providing service " + s);
			var serv = myCard._services[s]; 
			if (!serv)
				throw new Error("cannot provide service " + s);
			Flasck.provideService(postbox, services, s, new FlasckWrapper.Processor(wrapper, serv));
		}
	//	console.log("These services are available:", services);
		
		wrapper.cardCreated(myCard);
	} else if (cardInfo.mode === 'remote') {
		function connectPB(name, id) {
			console.log("need to show " + id + " through " + name);
		}
		
	    var url = cardInfo.url;
	    var idx = url.indexOf("?");
	    if (idx == -1)
	    	url = url + "?";
	    else
	    	url = url + "&";
	    var newpb = Math.random().toString().substring(2);
		postbox.remote(newpb, function(name) { connectPB(name, s); });
	    url += "postbox="+newpb +"&myAddr="+myAddr;
		inside.innerHTML = "<iframe src='" + url + "'></iframe>";
	} else
		throw new Error("Cannot handle card creation mode: " + cardInfo.mode);
	
	return handle;
}
// This is the thing that "represents" the card on the container side
FlasckHandle = function(postbox, myAddr) {
	this._ctor = 'FlasckHandle';
	this._isDisposed = false;
	this.postbox = postbox;
	this.myAddr = myAddr;
	this.channels = {};
	this.pending = [];
}

FlasckHandle.prototype.send = function(ctr, method /* args */) {
	var args = [];
	for (var i=2;i<arguments.length;i++)
		args[i-2] = arguments[i];
	var msg = { from: this.myAddr, method: method, args: args };
	if (this.pending) { // we can't send messages until established, so just queue them in order
		msg.ctr = ctr;
		this.pending.push(msg);
		return;
	}
	if (!this.channels[ctr])
		throw new Error("There is no channel for contract " + ctr);
	var chan = this.channels[ctr];
	this.postbox.deliver(chan, msg);
}

FlasckHandle.prototype.redrawInto = function(into) {
	if (this.channels['org.flasck.Render']) {
		var msg = {};
		if (this.postbox.isLocal(this.channels['org.flasck.Render']))
			msg.into = into;
		// TODO: it seems the remote case needs something more here, but not quite sure what
		// might be on the other side
		this.send('org.flasck.Render', "render", msg);
	}
}

FlasckHandle.prototype.dispose = function() {
	this._isDisposed = true;
	if (this.channels['org.flasck.Init'])
		this.send('org.flasck.Init', 'dispose');
	this.postbox.remove(this.myAddr);
}
FlasckWrapper = function(postbox, initSvc, cardClz, inside, cardId) {
	this._ctor = 'FlasckWrapper';
	this.postbox = postbox;
	this.initSvc = initSvc;
	this.cardClz = cardClz;
	this.cardId = cardId;
	this.ctrmap = {};
	this.nodeCache = {};
	this.cardCache = {};
	this.card = null; // will be filled in later
	this.ports = [];
	this.div = inside;
	this.updateAreas = [];
	return this;
}

FlasckWrapper.Processor = function(wrapper, service) {
	if (!service)
		throw new Error("No service was defined");
	this.wrapper = wrapper;
	this.service = service;
}

FlasckWrapper.Processor.prototype.process = function(message) {
	"use strict"
//	console.log("received message", message);
	var meth = this.service[message.method];
	if (!meth)
		throw new Error("There is no method '" + message.method +"'");
	var args = [];
	for (var i=0;i<message.args.length;i++)
		args.push(FLEval.fromWireService(message.from, message.args[i], this.wrapper));
	var clos = meth.apply(this.service, args);
	if (clos) {
		this.wrapper.messageEventLoop(FLEval.full(clos));
	}
}

FlasckWrapper.prototype.editField = function(ev, elt, rules, inside) {
	var self = this;
	var doc = elt.ownerDocument;
	var ct = elt.childNodes[0].wholeText; // should just be text, I think ...
	elt.innerHTML = '';
	var input = doc.createElement("input");
	input.setAttribute("type", "text");
	input.value = ct;
	input.select();
	input.onblur = function(ev) { self.saveField(ev, elt, rules, null, inside); }
	input.onkeyup = function(ev) { if (ev.keyCode == 13) { input.blur(); /* self.saveField(ev, elt, rules, null, inside); */ return false;} }
	input.onkeydown = function(ev) { if (ev.keyCode == 27) { self.saveField(ev, elt, rules, ct, inside); return false; } }
	elt.appendChild(input); 
	input.focus(); 
	elt.onclick = null;
}

FlasckWrapper.prototype.saveField = function(ev, elt, rules, revertTo, inside) {
	var self = this;
	var doc = elt.ownerDocument;
	var input = revertTo || elt.children[0].value;
	if (revertTo == null) {
		console.log("rules =", rules);
		// TODO: may need to do final validity checking
		// if (!rules.validate(input)) { ... }
		rules.save.call(this.card, this, inside, input);
	}
	elt.innerHTML = '';
	var text = doc.createTextNode(input);
	elt.appendChild(text);
	elt.onclick = function(ev) { self.editField(ev, elt, rules, inside); }
}

FlasckWrapper.prototype.editableField = function(elt, rules, inside) {
	var self = this;
//	console.log("registering field", elt.id, "as subject to editing");
	elt.className += " flasck-editable";
	elt.flasckEditMode = false; 
	elt.onclick = function(ev) { self.editField(ev, elt, rules, inside); }
}

// TODO: may also need "saveState", or could add another condition in here
FlasckWrapper.prototype.saveObject = function(obj) {
	if (obj === this.card) {
//		console.log("is this an attempt to save state?");
		return;
	}
	if (!obj.id) {
		console.log("cannot save object without an id");
		return;
	}
	if (!obj._fromService) {
		console.log("cannot automatically save object", obj, "because it does not have a _fromService tag.  Is it new?  a sub-object?  a new case?");
		return;
	}
	
	// TODO: this may seem slightly more complex than it "needs" to be, but it considers the case that it's a different service that doesn't just have "save"
	// also, it means we don't have to store two pointers, remote & local
	if (this.contractInfo['org.ziniki.KeyValue'].service._addr === obj._fromService)
		service = this.contractInfo['org.ziniki.KeyValue'].service;
	else if (this.contractInfo['org.ziniki.Persona'].service._addr === obj._fromService)
		service = this.contractInfo['org.ziniki.Persona'].service;
	else {
		console.log("don't know how to save to service associated with", obj);
		return;
	}
	this.postbox.deliver(obj._fromService, {from: service._myaddr, method: "save", args: [obj] });
}

FlasckWrapper.prototype.cardCreated = function(card) {
	var self = this;
	this.card = card;
	this.services = {};
	for (var svc in card._services) {
		var svcAddr = this.postbox.newAddress();
		this.postbox.register(svcAddr, new FlasckWrapper.Processor(this, card._services[svc]));
		this.services[svc] = this.postbox.unique(svcAddr);
	}
	var userInit;
	var userCroset; // not needed ...
	var kvupdate;
	// After long deliberation, this is NOT a hack
	// This is more by way of a proxy or an impedance-matching layer
	// Another way of thinking about it is that it is doing "boilerplate" things that are required by the underlying Flasck/Card paradigm, but that FLAS users shouldn't need to do
	// For example:
	//  * the initial handshaking about services
	//  * rewriting object IDs when Ziniki creates them
	//  * monkeying around with Croset changes
	var contracts = {};
	for (var ctr in card._contracts) {
		contracts[ctr] = new FlasckWrapper.Processor(this, card._contracts[ctr]);
		if (ctr === 'org.flasck.Init')
			userInit = contracts[ctr];
		else if (ctr == 'org.flasck.Render')
			throw new Error("Users cannot define " + ctr);
	}
	contracts['org.flasck.Init'] = {
		process: function(message) {
			"use strict";
			if (message.method === 'services')
				this.services(message.from, message.args[0]);
			else if (message.method === 'state')
				this.state(message.from, message.args[0]);
			else if (message.method === 'loadId')
				this.loadId(message.from, message.args[0]);
			else if (message.method == 'dispose')
				this.dispose(message.from);
			else
				throw new Error("Cannot process " + message.method);
		},
		services: function(from, serviceMap) {
			"use strict";
			for (var ctr in serviceMap) {
				self.services[ctr] = serviceMap[ctr];
				if (card._contracts[ctr])
					card._contracts[ctr]._addr = serviceMap[ctr];
			}
		},
		state: function(from, state) {
			"use strict";
//			console.log("Setting state");
			var clos;
			if (state)
				; // restore it somehow
			else
				// If there is no existing state, call initializers
				clos = card._onReady();
				
			// process anything that comes back
			if (clos) {
				self.messageEventLoop(FLEval.full(clos));
			}
				
			// OK ... I claim it's ready now
			if (userInit && userInit.service.onready) {
				userInit.process({from: from, method: 'onready', args: []});
			}
		},
		loadId: function(from, id) {
			var uf = function(obj) {
				if (userInit && userInit.service.update)
					userInit.process({from: from, method: 'update', args: [obj]});
				else
					console.log("there is no update method to handle", id, type, obj);
			};
			if (self.services['org.ziniki.KeyValue']) {
				var proxy = new FlasckWrapper.Processor(self, { update: uf });
				var ha = self.postbox.newAddress();
				self.postbox.register(ha, proxy);
				var uq = self.postbox.unique(ha);
				self.ports.push(uq);
				var handler = { type: 'handler', chan: uq };
				// not sure to what extent this is a hack ...
				if (id.substring(0, 11) === 'personafor/') {
					var next = id.substring(11);
					var idx = next.indexOf('/');
					var appl = next.substring(0, idx);
					next = next.substring(idx+1);
					idx = next.indexOf('/');
					if (idx >= 0)
						next = next.substring(0, idx);
					self.postbox.deliver(self.services['org.ziniki.Persona'], {from: self.ctrmap['org.flasck.Init'], method: 'forApplication', args:[appl, next, handler] });
				} else if (id.substring(0, 9) === 'resource/') {
					self.postbox.deliver(self.services['org.ziniki.KeyValue'], {from: self.ctrmap['org.flasck.Init'], method: 'resource', args:[id, handler] });
				} else if (id.substring(0, 6) === 'typed/') {
					idx = id.indexOf('/', 6);
					if (idx < 0)
						throw new Error("Invalid id in typed request: " + id);
					var type = id.substring(6, idx);
					id = id.substring(idx+1);
					self.postbox.deliver(self.services['org.ziniki.KeyValue'], {from: self.ctrmap['org.flasck.Init'], method: 'typed', args:[type, id, handler] });
				} else if (id.substring(0, 12) === 'unprojected/') {
					id = id.substring(12);
					self.postbox.deliver(self.services['org.ziniki.KeyValue'], {from: self.ctrmap['org.flasck.Init'], method: 'unprojected', args:[id, handler] });
				} else
					throw new Error("Cannot understand what you want me load: " + id);
			}
		},
		dispose: function(from) {
			// This stops the bleeding; I think there is more we need to do, probably more we need to do *automatically*, e.g. canceling subscriptions to ZiNC
			for (var c in self.ports) {
				var port = self.ports[c];
				if (port)
					postbox.remove(port);
			}
		},
		service: {} // to store _myaddr
	}
	contracts['org.flasck.Render'] = {
		process: function(message) {
			"use strict";
			if (message.method === 'render')
				this.render(message.from, message.args[0]);
			else
				throw new Error("Cannot process " + message.method);
		},
		render: function(from, opts) {
			"use strict";
			if (!self.card._render)
				console.log("There is no method _render on ", self.card);
			else {
				if (opts.into)
					self.div = opts.into;
				self.card._render.call(self.card, self.div.ownerDocument, self, self.div);
			}
		},
		service: {} // to store _myaddr
	}
	// END OF PROXY DEFINITIONS
	for (var ctr in contracts) {
		var ctrAddr = this.postbox.newAddress();
		this.postbox.register(ctrAddr, contracts[ctr]);
		var uq = this.postbox.unique(ctrAddr);
		this.ports.push(uq);
		this.ctrmap[ctr] = uq;
		contracts[ctr].service._myaddr = uq;
	}
	this.contractInfo = contracts;
	this.postbox.deliver(this.initSvc, {from: this.ctrmap['org.flasck.Init'], method: 'ready', args:[this.ctrmap]});
}

FlasckWrapper.prototype.dispatchEvent = function(handler, ev) {
//	console.log("dispatching event of type", ev.type);
	var msgs = FLEval.full(new FLClosure(this.card, handler, [FLEval.makeEvent(ev)]));
	this.messageEventLoop(msgs);
}

FlasckWrapper.prototype.messageEventLoop = function(flfull) {
	var msgs = FLEval.flattenList(flfull);
	var todo = [];
	while (msgs && msgs.length > 0) {
		msgs = FLEval.flattenList(FLEval.full(this.processMessages(msgs, todo)));
	}
	this.updateDisplay(todo);
}

FlasckWrapper.prototype.processMessages = function(msgs, todo) {
//	console.log("processing messages", msgs);
	if (!todo)
		todo = {};
	var momsgs = [];
	for (var i=0;i<msgs.length;i++) {
		var hd = msgs[i];
		var mo = null;
		console.log("Processing message", hd);
		if (hd._ctor === 'Nil')
			;
		else if (hd._ctor === 'Cons')
			mo = this.processMessages(FLEval.flattenList(hd), todo);
		else
			mo = this.processOne(hd, todo);
		if (mo)
			momsgs = momsgs.concat(FLEval.flattenList(FLEval.full(mo)));
	}
	return momsgs;
}

FlasckWrapper.prototype.processOne = function(msg, todo) {
//	console.log("Message: ", msg);
	if (msg._ctor === 'Send') {
		var target = msg.target;
		if (target === null || target === undefined) {
			return new FLError("cannot have undefined target");
		}
		if (typeof target === 'string') {
			target = this.card[target];
			if (target instanceof FLClosure) {
				target = FLEval.full(this.card[target]);
				this.card[msg.target] = target; // if _we_ had to evaluate it, store the output so we don't repeat the evaluation
			}
		}
		if (!target._special) {
			return new FLError("Target for send is not 'special': " + msg.target);
		}
		var meth = msg.method;
		if (target._special === 'contract') {
			var args = [];
			var l = msg.args;
			while (l && l._ctor === 'Cons') {
				args.push(FLEval.toWire(this, l.head));
				l = l.tail;
			}
			var addr = target._addr;
			if (!addr) {
				return new FLError("No service was provided for " + target._contract);
			}
			this.postbox.deliver(addr, {from: target._myaddr, method: meth, args: args });
		} else if (target._special === 'object') {
			var args = FLEval.flattenList(msg.args);
			var actM = target[meth];
			if (!actM) {
				return new FLError("There is no method " + meth + " on ", target);
			}
			return actM.apply(target, args);
		} else {
			return new FLError("Cannot handle special case: " + target._special);
		}
	} else if (msg._ctor === 'Assign') {
		var into = msg.target;
		if (!into)
			into = this.card;
		if (msg.value._ctor === 'MessageWrapper') {
			into[msg.field] = msg.value.value;
			todo.push(Assign(into, msg.field, msg.value.value));
			return msg.value.msgs;
		}
		into[msg.field] = msg.value;
		todo.push(msg);
	} else if (msg._ctor === 'CrosetInsert' || msg._ctor === 'CrosetReplace' || msg._ctor === 'CrosetRemove' || msg._ctor === 'CrosetMove') {
		var meth;
		switch (msg._ctor) {
		case 'CrosetInsert':
			meth = 'insert';
			args = [msg.target.crosetId, msg.key.toString(), msg.key.id];
			break;
		case 'CrosetMove':
			meth = 'move';
			args = [msg.target.crosetId, msg.from.id, msg.from.toString(), msg.to.toString()];
			break;
		case 'CrosetRemove':
			if (msg.forReal) {
				meth = 'delete';
				args = [msg.target.crosetId, msg.key.toString(), msg.key.id];
			}
			// otherwise this is just removing it from the local copy ... should we actually make these different messages?
			break;
		case 'CrosetReplace':
			// This is just a change to the actual object, which should be separately recorded; the Croset does not change
			break;
		default:
			return new FLError("don't handle" + msg);
		}
		if (meth)
			this.postbox.deliver(this.services['org.ziniki.CrosetService'], {from: this.contractInfo['org.ziniki.CrosetService'].service._myaddr, method: meth, args: args });
		todo.push(msg);
	} else if (msg._ctor === 'CreateCard') {
		// If the user requests that we make a new card in response to some action, we need to know where to place it
		// The way we fundamentally know this is to look at the "where" option
		var options = FLEval.flattenMap(msg.options);
		var where = options.where;
		delete options.where;
		if (!where)
			throw new Error("Can't display a card nowhere");
		else if (where === 'overlay') {
			var overlay = this.div.ownerDocument.getElementById('flasck_popover_div');
            this.showCard(overlay, options);
            var popover = this.div.ownerDocument.getElementById('flasck_popover');
            if (!popover.isOpen)
            	popover.showModal();
   		} else {
   			// assume that 'where' is the name of a div
			var div = this.div.ownerDocument.getElementById(where);
   			this.showCard(div, options); 
		}
	} else if (msg._ctor == 'Debug') {
		var val = FLEval.full(msg.value);
		console.log("Debug:", val);
	} else
		return new FLError("The method message " + msg._ctor + " is not supported");
}

FlasckWrapper.prototype.convertSpecial = function(obj) {
	if (!obj._onchan) {
		if (obj._special === 'handler') {
			var proxy = new FlasckWrapper.Processor(this, obj);
			// 2017-10-25 It seems to me when looking at this while trying to replicate for JVM,
			// that we should assign this address on construction, not at send
			// otherwise we could end up with multiple registrations for the same recipient
			var ha = this.postbox.newAddress();
			this.postbox.register(ha, proxy);
			obj._myaddr = this.postbox.unique(ha);
			this.ports.push(obj._myaddr);
		} else
			throw new Error("Cannot send an object of type " + a._special);
	}
	// TODO: I can't help feeling type should be "_type" or "_special" ... this is the wire format, after all
	return { type: obj._special, chan: obj._myaddr };
}

FlasckWrapper.prototype.onUpdate = function(op, obj, field, area, fn) {
	if (!obj) obj = this.card; // should we insist on getting the card by throwing an error if not?
	if (op === 'assign' && !fn)
		throw new Error("Must provide fn for assign");
// 	console.log("added update", this.updateAreas.length, ":", op, obj, field);
	this.updateAreas.push({op: op, obj: obj, field: field, area: area, fn: fn});
// 	console.log("updateAreas length =", this.updateAreas.length);
}

FlasckWrapper.prototype.removeOnUpdate = function(op, obj, field, area) {
	if (!obj) obj = this.card; // should we insist on getting the card by throwing an error if not?
	for (var i=0;i<this.updateAreas.length;) {
		var ua = this.updateAreas[i];
		if (ua.op == op && ua.area === area && ua.obj == obj && ua.field == field) {
			this.updateAreas.splice(i, 1);
//			console.log("removed update #", i, op, obj, field);
		} else
			i++;
	}
}

FlasckWrapper.prototype.removeActions = function(area) {
//	console.log("remove all actions that have area", area);
	for (var i=0;i<this.updateAreas.length;) {
		var ua = this.updateAreas[i];
		if (ua.area === area) {
			this.updateAreas.splice(i, 1);
			console.log("removed update #", i, ua.op, ua.obj, ua.field);
		} else
			i++;
	}
}

FlasckWrapper.prototype.updateDisplay = function(todo) {
	if (!this.div || todo.length == 0)
		return; // need to set up render contract first
		
	// TODO: there is a "premature" optimization step here where we try and avoid duplication
	var doc = this.div.ownerDocument;
	for (var t=0;t<todo.length;t++) {
		var item = todo[t];
		if (item instanceof _Assign) {
//			console.log("Assign");
			var target = item.target || this.card;
			for (var i=0;i<this.updateAreas.length;i++) {
				var ua = this.updateAreas[i];
				if (ua.op != 'assign') continue;
				if (ua.field != item.field || ua.obj != target)
					continue;
//				console.log("assign", i, ua.area, target, item.field, obj);
				ua.fn.call(ua.area, target[item.field]);
			}
		} else if (item instanceof _CrosetInsert) {
//			console.log("Croset Insert");
			for (var i=0;i<this.updateAreas.length;i++) {
				var ua = this.updateAreas[i];
				if (ua.op != 'croset' || ua.obj != item.target)
					continue;
				var child = ua.area._newChild();
				child._crokey = item.key;
				ua.area._insertItem(child);
				
				// Hard question: what do we do when we have "inserted" something of nothing?
				// i.e. we have created a "member" with a key and an ID, but nothing in the hash?
				// I am currently taking the option to send across "just the id"
				var obj = item.target.memberOrId(item.key);

				// Either way, pass the object
				child._assignToVar(obj);
			}
		} else if (item instanceof _CrosetReplace) {
//			console.log("Croset Replace");
			var obj = item.target.member(item.key);
			for (var i=0;i<this.updateAreas.length;i++) {
				var ua = this.updateAreas[i];
				if (ua.op != 'crorepl') continue;
				if (ua.field != obj.id || ua.obj != item.target)
					continue;
//				console.log("crorepl", i, ua.area, item.target, obj);
				ua.area._assignToVar(obj);
			}
		} else if (item instanceof _CrosetRemove) {
//			console.log("Croset Remove");
			for (var i=0;i<this.updateAreas.length;i++) {
				var ua = this.updateAreas[i];
				if (ua.op != 'croset') continue;
				if (ua.obj != item.target)
					continue;
				ua.area._deleteItem(item.key);
			}
		} else if (item instanceof _CrosetMove) {
//			console.log("Croset Move");
			for (var i=0;i<this.updateAreas.length;i++) {
				var ua = this.updateAreas[i];
				if (ua.op != 'croset') continue;
				if (ua.obj != item.target)
					continue;
				ua.area._moveItem(item.from, item.to);
			}
		} else
			throw new Error("Cannot handle item " + item);
	}
}

FlasckWrapper.prototype.showCard = function(into, cardOpts) {
	if (!cardOpts.mode)
		cardOpts.mode = 'local';
	if (!into)
		throw new Error("Must specify a div to put the card into");
	into.innerHTML = '';
	/* I accept the intent of this, but I don't see how it works - if it works
	var uid = into.id;
	if (this.cardCache[uid] && !this.cardCache[uid]._isDisposed) {
   		this.cardCache[uid].redrawInto(into);
   		return this.cardCache[uid];
	} else {
	*/
  		var svcs = cardOpts.services;
  		if (!svcs || svcs._ctor === 'Nil')
	  		svcs = this.services;
  		var innerCard = Flasck.createCard(this.postbox, into, cardOpts, svcs);
//  		this.cardCache[uid] = innerCard;
  		return innerCard;
//	}
}

function d3attrFn(card, flfn) {
    return function(d, i) {
        var elt = { _ctor: 'D3Element', data: d, idx: i }
        return FLEval.full(flfn.call(card, elt));
    }
}

FlasckWrapper.prototype.updateD3 = function(svg, info) { // TODO: other args
	info = FLEval.full(info);
	// info is an assoc of key -> value
	// info.data is a function returning the list of data items (of any type; that's up to the user code to sort out)
    var mydata = FLEval.flattenList(StdLib.assoc(info, "data").call(this.card));
    
    // info.enter is a list of zero-or-more 'enter' methods on the card each of which is () -> [D3Action] 
    var enter = StdLib.assoc(info, "enter");
    var cmds = [];
    while (enter._ctor === 'Cons') {
        var a = enter.head;
        var v = FLEval.full(a.apply(this.card));
        cmds.push({ select: v.head.args.head, insert: v.head.args.head });
        enter = enter.tail;
    }
    
    // info.layout is a list of zero-or-more layouts on the card, each of which is a pair of (pattern, [prop]) where each prop is a pair (name, value-or-function)
    var layout = StdLib.assoc(info, "layout");
    for (var c in cmds)
        d3.select(svg).selectAll(cmds[c].select).data(mydata).enter().append(cmds[c].insert);
    while (layout._ctor === 'Cons') {
        var mine = layout.head;
        var patt = mine.members[0];
        var props = mine.members[1];
        var actOn = d3.select(svg).selectAll(patt);
        while (props._ctor === 'Cons') {
            var ph = props.head;
            var attr = ph.members[0];
            if (attr === 'text')
                    actOn = actOn.text(d3attrFn(this.card, ph.members[1]));
            else {
                    if (attr === 'textAnchor')
                            attr = 'text-anchor';
                    else if (attr === 'fontFamily')
                            attr = 'font-family';
                    else if (attr === 'fontSize')
                            attr = 'font-size';
                    actOn = actOn.attr(attr, d3attrFn(this.card, ph.members[1]));
            }
            props = props.tail;
        }
        layout = layout.tail;
    }
}

var CardArea = function(pdiv, wrapper, card) {
	"use strict";
	this._parent = null;
	this._wrapper = wrapper;
	this._doc = pdiv.ownerDocument;
	this._mydiv = pdiv;
	this._card = card;
}

var uniqid = 1;

var Area = function(parent, tag, ns, myhtml, mydiv) {
	"use strict";
	if (parent) {
		this._parent = parent;
		this._wrapper = parent._wrapper;
		this._doc = parent._doc;
		this._indiv = parent._mydiv;
		if (myhtml) {
			var t = document.createElement('template');
			t.innerHTML = myhtml.trim(); 
		    this._mydiv = t.content.firstChild;
		} else if (mydiv) {
			this._mydiv = mydiv;
		} else if (tag) {
			if (ns)
				this._mydiv = this._doc.createElementNS(ns, tag);
			else
				this._mydiv = this._doc.createElement(tag);
		}
		if (this._mydiv) {
			this._mydiv._area = this;
			if (!mydiv) {
				this._mydiv.setAttribute('id', this._wrapper.cardId+'_'+(uniqid++));
				this._indiv.appendChild(this._mydiv);
			}
		}
		this._card = parent._card;
	}
}

Area.prototype._clear = function() {
	this._mydiv.innerHTML = '';
}

Area.prototype._onAssign = function(obj, field, fn) {
	"use strict";
	this._wrapper.onUpdate("assign", obj, field, this, fn);
}

var DivArea = function(parent, tag, ns, myhtml, mydiv) {
	"use strict";
	if (!tag && !myhtml && !mydiv)
		tag = 'div';
	Area.call(this, parent, tag, ns, myhtml, mydiv);
	this._interests = [];
}

DivArea.prototype = new Area();
DivArea.prototype.constructor = DivArea;

DivArea.prototype._interested = function(obj, fn) {
	this._interests.push({obj: obj, fn: fn});
	fn.call(obj);
}

DivArea.prototype._fireInterests = function() {
	for (var i=0;i<this._interests.length;i++) {
		var ii = this._interests[i];
		ii.fn.call(ii.obj);
	}
}

DivArea.prototype._makeDraggable = function() {
	this._mydiv.setAttribute('draggable', 'true');
	this._mydiv['ondragstart'] = function(event) {
		_Croset.listDrag(event);
	}
}

DivArea.prototype._dropSomethingHere = function(contentTypes) {
	function isAcceptable(e) {
        var files = e.dataTransfer.files;
        var acceptable = false;
        for (var i=0;i<files.length; i++) {
        	for (var j=0;j<contentTypes.length;j++)
	            if (files[i].type.match(contentTypes[j]))
    	        	return files[i];
        }
        return null;
	}
	this._mydiv.addEventListener('dragover', function(e) {
        e.stopPropagation();
		e.preventDefault();
        if (isAcceptable(e)) {
	        e.dataTransfer.dropEffect = 'copy';
	    }
	});
	var mydiv = this._mydiv;
    this._mydiv.addEventListener('drop', function(e) {
        e.stopPropagation();
        e.preventDefault();
        var file = isAcceptable(e);
        if (file) {
        	mydiv.innerHTML = '';
            var reader = new FileReader();
            reader.onload = function(e2) { // finished reading file data.
                var img = document.createElement('img');
                img.src = reader.result;
                mydiv.appendChild(img);
                if (mydiv["on_drop"])
                	mydiv['on_drop'](new org.flasck.DropEvent(file));
            }
            reader.readAsDataURL(file); // start reading the file data.
		}
	});
}

var ListArea = function(parent, tag) {
	"use strict";
	Area.call(this, parent, tag || 'ul');
}

ListArea.prototype = new Area();
ListArea.prototype.constructor = ListArea;

ListArea.prototype._assignToVar = function(croset) {
	"use strict";
	this._wrapper.removeOnUpdate("croset", this._croset, null, this);
	this._croset = croset;
	this._clear();
	if (croset && !(croset instanceof FLError)) {
   		if (croset._ctor !== 'Croset') throw new Error('ListArea logic only handles Crosets right now');
    		var off = 0;
    		for (var pos=0;pos<10;pos++) {
    			if (pos+off >= croset.length())
    				break;
    			var ck = croset.key(pos+off);
    			var child = this._newChild();
    			child._crokey = ck;
    			this._insertItem(child);
    			child._assignToVar(croset.at(pos+off));
  		}
  		this._wrapper.onUpdate("croset", croset, null, this);
	}
}

ListArea.prototype._insertItem = function(child) {
  	"use strict";
	if (!child._crokey)
		throw new Error("Cannot handle null _crokey in " + child);
	for (var i=0;i<this._mydiv.children.length;i++) {
		var a = this._mydiv.children[i];
		if (this._croset.before(child._crokey, a._area._crokey)) {
			this._mydiv.insertBefore(child._mydiv, a);
			return;
		}
	}
	// if we reached the end
	this._mydiv.appendChild(child._mydiv);
}

ListArea.prototype._deleteItem = function(key) {
  	"use strict";
	for (var i=0;i<this._mydiv.children.length;i++) {
		var a = this._mydiv.children[i];
		if (key.compare(a._area._crokey) == 0) {
			this._mydiv.removeChild(a);
			return;
		}
	}
}

ListArea.prototype._moveItem = function(from, to) {
  	"use strict";
  	// I believe the semantics of appendChild/insertBefore mean that this is in fact unnecessary ...
//  	console.log("moving from", from, "to", to);
  	var removeDiv, beforeDiv;
	for (var i=0;i<this._mydiv.children.length;i++) {
		var a = this._mydiv.children[i];
		if (from.compare(a._area._crokey) == 0) {
			removeDiv = a;
			if (beforeDiv) break;
		}
		if (!beforeDiv && to.compare(a._area._crokey) <= 0) {
			beforeDiv = a;
			if (removeDiv) break;
		}
	}
//	console.log("move", removeDiv, "before", beforeDiv);
	if (beforeDiv == removeDiv) return;
	if (beforeDiv)
		this._mydiv.insertBefore(removeDiv, beforeDiv)
	else
		this._mydiv.appendChild(removeDiv);
	removeDiv._area._crokey = to;
}

/*
ListArea.prototype._format = function() {
	for (var c=0;c<this._mydiv.children.length;c++) {
		this._mydiv.children[c]._area.formatItem();
	}
}
*/

ListArea.prototype._supportDragging = function() {
	var ul = this._mydiv;
	var wrapper = this._wrapper;
	this._mydiv['ondragover'] = function(event) {
		_Croset.listDragOver(event, ul);
	}
	this._mydiv['ondrop'] = function(event) {
		var msgs = _Croset.listDrop(event, ul);
		wrapper.messageEventLoop(msgs);
	}
}
	
var TextArea = function(parent, tag) {
	"use strict";
	Area.call(this, parent, tag || 'span');
}

TextArea.prototype = new Area();
TextArea.prototype.constructor = TextArea;

TextArea.prototype._setText = function(text) {
	"use strict";
//	console.log("setting text to", text);
	var tmp = this._doc.createTextNode(text);
	this._mydiv.innerHTML = '';
	this._mydiv.appendChild(tmp);
}

TextArea.prototype._insertHTML = function(e) {
	"use strict";
	e = FLEval.full(e);
	if (e === null || e === undefined || e instanceof FLError)
		e = "";
	this._mydiv.innerHTML = e.toString();
}

TextArea.prototype._assignToText = function(e) {
	"use strict";
	e = FLEval.full(e);
	if (e === null || e === undefined || e instanceof FLError)
		e = "";
	this._setText(e.toString());
}

TextArea.prototype._edit = function(ev, rules, containingObj) {
	var self = this;
	var ct = "";
	if (this._mydiv.childNodes.length > 0)
		ct = this._mydiv.childNodes[0].wholeText;
	this._mydiv.innerHTML = '';
	var input = this._doc.createElement("input");
	input.setAttribute("type", "text");
	input.value = ct;
	input.select();
	input.onblur = function(ev) { self._save(ev, rules, null, containingObj); }
	input.onkeyup = function(ev) { if (ev.keyCode == 13) { input.blur(); ev.preventDefault(); } }
	input.onkeydown = function(ev) { if (ev.keyCode == 27) { self._save(ev, rules, ct, containingObj); ev.preventDefault(); } }
	this._mydiv.appendChild(input); 
	input.focus(); 
	this._mydiv.onclick = null;
}

TextArea.prototype._save = function(ev, rules, revertTo) {
	var self = this;
	var input = revertTo || this._mydiv.children[0].value;
	if (revertTo == null) {
		// TODO: may need to do final validity checking
		// if (!rules.validate(input)) { ... }
		rules.save.call(this, this._wrapper, input);
	}
	this._mydiv.innerHTML = '';
	var text = this._doc.createTextNode(input);
	this._mydiv.appendChild(text);
	this._mydiv.onclick = function(ev) { self._edit(ev, rules); }
}

TextArea.prototype._editable = function(rules) {
	var self = this;
//	console.log("registering field", elt.id, "as subject to editing");
//	this._mydiv.flasckEditMode = false; 
	this._mydiv.onclick = function(ev) { self._edit(ev, rules); }
}

var CardSlotArea = function(parent, cardOpts) {
	"use strict";
	Area.call(this, parent, 'div');
	if (parent && cardOpts)
		this._wrapper.showCard(this._mydiv, cardOpts);
}

CardSlotArea.prototype = new Area();
CardSlotArea.prototype.constructor = CardSlotArea;

CardSlotArea.prototype._updateToCard = function(card) {
	if (card) {
		var ex = card.explicit;
		if (typeof ex === 'string')
			ex = getPackagedItem(ex);
		if (ex) {
			var opts = { explicit: ex };
			if (card.loadId)
				opts['loadId'] = card.loadId;
			this._wrapper.showCard(this._mydiv, opts);
		} else {
			console.log("There is no card called", card.explicit);
			// we should clear out the card
		}
	}
	// otherwise we should clear out the card
}

var CasesArea = function(parent) {
	"use strict";
	Area.call(this, parent, 'div');
}

CasesArea.prototype = new Area();
CasesArea.prototype.constructor = CasesArea;

CasesArea.prototype._setTo = function(fn) {
	if (this._current == fn)
		return;
	this._current = fn;
	this._mydiv.innerHTML = '';
	var r = new Object();
	fn.call(r, this);
}

var D3Area = function(parent, cardOpts) {
	"use strict";
	Area.call(this, parent);
	if (parent) {
		this._data = FLEval.full(this._card._d3init_chart());
		this._onUpdate();
	}
}

D3Area.prototype = new Area();
D3Area.prototype.constructor = D3Area;

D3Area.prototype._onUpdate = function() {
	this._wrapper.updateD3(this._indiv, this._data);
}


// The Standard Library, exported under the "package" StdLib

function StdLib() {
}

StdLib.concat = function(l) {
	var ret = "";
	while (true) {
		l = FLEval.head(l);
		if (l._ctor == 'Cons') {
			var head = FLEval.full(l.head);
			ret += head;
			l = l.tail;
		} else
			break;
	}
	return ret;
}

asString = function(any) {
	if (!any) return "";
	return any.toString();
}

append = function(s1, s2) {
	return FLEval.full(s1) + FLEval.full(s2);
}

join = function(l, isep) {
	var ret = "";
	var sep = "";
	while (true) {
		l = FLEval.head(l);
		if (l._ctor == 'Cons') {
			var head = FLEval.full(l.head);
			if (head) {
				ret += sep + head;
				sep = isep;
			}
			l = l.tail;
		} else
			break;
	}
	return ret;
}

// The standard library "filter" function, which can be imagined as:
// filter f [] = []
// filter f (a:l)
//   if (f a) => a:(filter f l)
//   else => filter f l

StdLib.filter = function(f, al) {
	al = FLEval.head(al);
	if (al instanceof FLError)
		return al;
	if (al instanceof List) {
		if (al.__ctor == 'Nil')
			return Nil;
		f = FLEval.head(f);
		if (f instanceof FLError)
			return f;
		if (typeof f === 'function') {
			var b = FLEval.head(f.apply(undefined, [al.head]));
			if (b) {
				return FLEval.closure(
					Cons,
					al.head,
					FLEval.closure(
						StdLib.filter,
						f,
						al.tail
					)
				);
			} else {
				return FLEval.closure(
					StdLib.filter,
					f,
					al.tail
				);
			}
		}
	}
}

// We still need to decide what to do about arrays and the like
// In general, we expect "list" but Crokeys in particular doesn't want to play that game
map = function(f,l) {
	"use strict"
	var l = FLEval.head(l);
	if (l === undefined || l === null)
		return Nil;
	if (l instanceof Array)
		return StdLib._mapArray(f, l);
	if (l._ctor !== 'Cons')
		return Nil;
	return Cons(FLEval.closure(f, l.head), FLEval.closure(map, f, l.tail));
}

StdLib._mapArray = function(f, arr) {
	"use strict";
	var ret = Nil;
	for (var i=arr.length-1;i>=0;i--)
		ret = Cons(FLEval.closure(f, arr[i]), ret);
	return ret;
}

// List comprehension for integers starting at n (and going to infinity)
intsFrom = function(n) {
	"use strict"
	return FLEval.closure(Cons, n, FLEval.closure(intsFrom, FLEval.closure(FLEval.plus, n, 1)));
}


StdLib.assoc = function(map, key) {
	"use strict"
	map = FLEval.head(map);
	key = FLEval.full(key);
	if (map._ctor === 'Assoc') {
		if (key === map.key)
			return map.value;
		else
			return StdLib.assoc(map.rest, key);
	}
}



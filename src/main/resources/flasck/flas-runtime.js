
// should this be part of Ziniki?
const ZiIdURI = function(s) {
    this.uri = s;
}
ZiIdURI.fromWire = function(cx, om, fields) {
    return new ZiIdURI(fields["uri"]);
}
ZiIdURI.prototype._towire = function(wf) {
    wf._wireable = "org.ziniki.common.ZiIdURI";
    wf.uri = this.uri;
}
const CommonEnv = function(bridge, broker) {
    if (!bridge) // when used as a constructor
        return;
    this.contracts = broker.contracts;
    this.structs = {};
    this.objects = {};
    this.objects['Random'] = Random;
    this.objects['FLBuiltin'] = FLBuiltin;
    this.objects['Crobag'] = Crobag;
    this.objects['CroEntry'] = CroEntry;
    this.objects['Image'] = Image;
    this.objects['org.ziniki.common.ZiIdURI'] = ZiIdURI; // hack that enables the Java name to be sent on the wire.  It probably shouldn't be; but should we send just a string or should we recognize ZiIdURI?
    this.objects['org.flasck.jvm.builtin.Crobag'] = Crobag; // hack that enables the Java name to be sent on the wire.  It probably shouldn't be.
    this.objects['org.flasck.jvm.builtin.CroEntry'] = CroEntry; // hack that enables the Java name to be sent on the wire.  It probably shouldn't be.
    this.objects['Calendar'] = Calendar;
    this.logger = bridge;
    this.broker = broker;
	this.nextDivId = 1;
	this.divSince = this.nextDivId;
	this.evid = 1;
    this.cards = [];
    this.queue = [];
    if (bridge.lock)
        this.locker = bridge;
    else
        this.locker = { lock: function() {}, unlock: function() {} };
}

CommonEnv.prototype.makeReady = function() {
    this.broker.register("Repeater", new ContainerRepeater());
}

CommonEnv.prototype.clear = function() {
	document.body.innerHTML = '';
}

CommonEnv.prototype.queueMessages = function(_cxt, msg) {
    this.locker.lock();
    this.queue.push(msg);
    var self = this;
    setTimeout(() => { self.dispatchMessages(_cxt); this.locker.unlock(); }, 0);
}

CommonEnv.prototype.dispatchMessages = function(_cxt) {
    var set = [];
    _cxt.updateCards = set;
    while (this.queue.length > 0) {
        var more = this.queue.shift();
        while (more && (!Array.isArray(more) || more.length > 0)) {
            more = this.handleMessages(_cxt, more);
        }
    }
    delete _cxt.updateCards;
    set.forEach(card => {
        card._updateDisplay(_cxt, card._renderTree);
        card._resizeDisplayElements(_cxt, card._renderTree);
    });
}

CommonEnv.prototype.handleMessages = function(_cxt, msg) {
    var msg = _cxt.full(msg);
    this.handleMessagesWith(_cxt, msg);
}

CommonEnv.prototype.handleMessagesWith = function(_cxt, msg) {
    msg = _cxt.full(msg);
    if (!msg)
        ;
    else if (msg instanceof FLError || typeof(msg) == 'string') {
        this.logger.log(msg);
        ;
    } else if (msg instanceof Array) {
        for (var i=0;i<msg.length;i++) {
            this.handleMessages(_cxt, msg[i]);
        }
	} else if (msg) {
        var ic = this.newContext();
        ic.updateCards = _cxt.updateCards;
        this.logger.log("dispatching message", msg);
        var m = msg.dispatch(ic);
        this.handleMessages(_cxt, m);
    }
}

CommonEnv.prototype.addAll = function(ret, m) {
    if (m) {
        if (Array.isArray(m)) {
            m.forEach(x => this.addAll(ret, x));
        } else
            ret.push(m);
    }
}

CommonEnv.prototype.newContext = function() {
	return new FLContext(this, this.broker);
}

if (typeof(window) !== 'undefined') {
    window.addEventListener('resize', function(ev) {
        if (window.maincard)
            window.maincard._resizeDisplayElements(env.newContext(), window.maincard._renderTree);
    });
}



const JSEnv = function(broker) {
	if (broker == null)
		broker = new SimpleBroker(console, this, {});
	var logger = {
		log: console.log,
		debugmsg: console.log		
	}
	CommonEnv.call(this, logger, broker);
	if (typeof(FlasckServices) !== 'undefined') {
		FlasckServices.configure(this);
	}
}

JSEnv.prototype = new CommonEnv();
JSEnv.prototype.constructor = JSEnv;



const ContractStore = function(_cxt) {
    this.env = _cxt.env;
    this.recorded = {};
    this.toRequire = {};
}

ContractStore.prototype.record = function(_cxt, name, impl) {
    this.recorded[name] = impl;
}

ContractStore.prototype.contractFor = function(_cxt, name) {
    const ret = this.recorded[name];
    if (!ret)
        throw new Error("There is no contract for " + name);
    return ret;
}

ContractStore.prototype.require = function(_cxt, name, clz) {
    const ctr = _cxt.broker.contracts[clz];
    const di = new DispatcherInvoker(this.env, _cxt.broker.require(clz));
    const px = proxy(_cxt, ctr, di);
    px._areYouA = function(cx, ty) { return ty === clz; }
    this.toRequire[name] = px;
}

ContractStore.prototype.required = function(_cxt, name) {
    const ret = this.toRequire[name];
    if (!ret)
        throw new Error("There is no provided contract for var " + name);
    return ret;
}

const DispatcherInvoker = function(env, call) {
    this.env = env;
    this.call = call;
}

DispatcherInvoker.prototype.invoke = function(meth, args) {
    // The context has been put as args 0; use it but pull it out
    // The handler will already have been patched in here, so pull it back out
    this.env.queueMessages(args[0], Send.eval(args[0], this.call, meth, args.slice(1, args.length-1), args[args.length-1]));
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

FLClosure.prototype.splitRWM = function(msgsTo) {
	this.msgsTo = msgsTo;
}

FLClosure.prototype.eval = function(_cxt) {
	if (this.val)
		return this.val;
	this.args[0] = _cxt;
	this.obj = _cxt.full(this.obj);
	if (this.obj instanceof FLError)
		return this.obj;
	if (this.fn instanceof FLError)
		return this.fn;
	var cnt = this.fn.nfargs();
	this.val = this.fn.apply(this.obj, this.args.slice(0, cnt+1)); // +1 for cxt
	if (typeof(this.msgsTo) !== 'undefined') {
		if (this.val instanceof ResponseWithMessages) {
			_cxt.addAll(this.msgsTo, ResponseWithMessages.messages(_cxt, this.val));
			this.val = ResponseWithMessages.response(_cxt, this.val);
		} else if (this.val instanceof FLClosure) {
			this.val.splitRWM(this.msgsTo);
		}
	}
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
	if (fn == null)
		throw Error("fn cannot be null");
	this.obj = obj;
	this.fn = fn;
	this.xcs = xcs;
	this.reqd = reqd;
	this.missing = [];
	for (var i=1;i<=reqd;i++) {
		if (!(i in xcs))
			this.missing.push(i);
	}
}

FLCurry.prototype.apply = function(_, args) {
	var _cxt = args[0];
	if (args.length == 1)
		return this; // nothing actually applied
	if (args.length-1 == this.missing.length) {
		var as = [_cxt];
		var from = 1;
		for (var i=1;i<=this.reqd;i++) {
			if (i in this.xcs)
				as[i] = this.xcs[i];
			else
				as[i] = args[from++];
		}
		var obj = _cxt.full(this.obj);
		return this.fn.apply(obj, as);
	} else {
		var miss = this.missing.slice(0);
		var xcs = {};
		for (var i in this.xcs)
			xcs[i] = this.xcs[i];
		for (var i=1;i<args.length;i++) {
			var m = miss.pop();
			xcs[m] = args[i];
		}
		return new FLCurry(this.obj, this.fn, this.reqd, xcs);
	}
}

FLCurry.prototype.nfargs = function() {
	return this.reqd;
}

FLCurry.prototype.toString = function() {
	return "FLCurry[" + this.reqd + "]";
}



const FLMakeSend = function(meth, obj, nargs, handler) {
	this.meth = meth;
	this.obj = obj;
	this.nargs = nargs;
	this.current = [];
	this.handler = handler;
}

FLMakeSend.prototype.apply = function(cx, args) {
	var all = this.current.slice();
	for (var i=1;i<args.length;i++)
		all.push(args[i]);
	if (all.length == this.nargs) {
		return Send.eval(cx, this.obj, this.meth, all, this.handler);
	} else {
		var ret = new FLMakeSend(this.meth, this.obj, this.nargs, this.handler);
		ret.current = all;
		return ret;
	}
}

FLMakeSend.prototype.nfargs = function() { return this.nargs; }

FLMakeSend.prototype.toString = function() {
	return "MakeSend[" + this.nargs + "]";
}



const FLContext = function(env, broker) {
	EvalContext.call(this, env, broker);
}

FLContext.prototype = new EvalContext();
FLContext.prototype.constructor = FLContext;

FLContext.prototype.addAll = function(ret, arr) {
	this.env.addAll(ret, arr);
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

FLContext.prototype.hash = function(...args) {
	var ret = {};
	for (var i=0;i<args.length;i++) {
		var hp = this.head(args[i]);
		if (!(hp instanceof HashPair))
			return new FLError("member was not a hashpair");
		var m = this.full(hp.m);
		ret[m] = hp.o;
	}
	return ret;
}

FLContext.prototype.applyhash = function(basic, hash) {
	basic = this.head(basic);
	if (basic instanceof FLError)
		return basic;
	hash = this.spine(hash);
	if (hash instanceof FLError)
		return hash;
	// TODO: we might need to clone basic before updating it, if it can be shared ...
	var okh = Object.keys(hash);
	for (var i=0;i<okh.length;i++) {
		var p = okh[i];
		if (!basic.state.has(p))
			return new FLError('cannot override member: ' + p);
		basic.state.set(p, hash[p]);
	}
	return basic;
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

FLContext.prototype.mksend = function(meth, obj, cnt, handler) {
	if (cnt == 0)
		return Send.eval(this, obj, meth, [], handler);
	else
		return new FLMakeSend(meth, obj, cnt, handler);
}

FLContext.returnNull = function(_cxt) {
	return null;
}
FLContext.prototype.mkacor = function(meth, obj, cnt) {
	if (cnt == 0) {
		if (typeof obj === 'undefined' || obj === null)
			return obj;
		else
			return this.oclosure(meth, obj);
	}
	else {
		if (typeof obj === 'undefined' || obj === null) {
			var fn = function(_cxt) {
				return null;
			}
			fn.nfargs = function() { return cnt; };
			return this.ocurry(cnt, fn, obj);
		} else
			return this.ocurry(cnt, meth, obj);
	}
}

FLContext.prototype.makeStatic = function(clz, meth) {
	const oc = this.objectNamed(clz);
	const ocm = oc[meth];
	const ret = function(...args) {
		return ocm.apply(null, args);
	};
	ret.nfargs = ocm.nfargs;
	return ret;
}

FLContext.prototype.head = function(obj) {
	while (obj instanceof FLClosure)
		obj = obj.eval(this);
	return obj;
}

FLContext.prototype.spine = function(obj) {
	obj = this.head(obj);
	if (obj instanceof FLError)
		return obj;
	if (Array.isArray(obj))
		return obj;
	if (obj.constructor === Object) {
		return obj;
	}
	throw Error("spine should only be called on lists");
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
	} else if (obj.state instanceof FieldsContainer) {
		var ks = Object.keys(obj.state.dict);
		for (var i=0;i<ks.length;i++) {
			obj.state.dict[ks[i]] = this.full(obj.state.dict[ks[i]]);
		}
	}
	return obj;
}

FLContext.prototype.isTruthy = function(val) {
	val = this.full(val);
	return !!val;
}

FLContext.prototype.isA = function(val, ty) {
	if (val instanceof Object && '_areYouA' in val) {
		return val._areYouA(this, ty);
	}
	switch (ty) {
	case 'Any':
		return true;
	case 'Boolean':
		return val === true || val === false;
	case 'True':
		return val === true;
	case 'False':
		return val === false;
	case 'Number':
		return typeof(val) == 'number';
	case 'String':
		return typeof(val) == 'string';
	case 'List':
		return Array.isArray(val);
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

FLContext.prototype.nextDocumentId = function() {
	return "flaselt_" + (this.env.nextDivId++);
}

FLContext.prototype.attachEventToCard = function(card, handlerInfo, div, wrapper) {
	const eventName = handlerInfo.event._eventName;
	if (div) {
		var id1 = this.env.evid++;
		// this.env.logger.log("adding handler " + id1 + " to " + div.id + " for " + eventName);
		var handler = ev => {
			// this.env.logger.log("firing handler " + id1 + " to " + div.id + " for " + eventName);
			const ecx = this.env.newContext();
			const fev = handlerInfo.event.eval(ecx);
			const evt = new FLEventSourceTrait(div, wrapper.value);
			fev["EventSource"] = evt;
			ecx.handleEvent(card, handlerInfo.handler, fev);
			ev.stopPropagation();
			ev.preventDefault();
		};
		div.addEventListener(eventName, handler);
		return handler;
	}
	return null;
}

FLContext.prototype.handleEvent = function(card, handler, event) {
	if (card && card._updateFromInputs) {
		card._updateFromInputs();
	}
	var reply = [];
	if (handler) {
		reply = handler.call(card, this, event);
	}
	reply.push(new UpdateDisplay(this, card));
	this.env.queueMessages(this, reply);
}

FLContext.prototype.localCard = function(cardClz, eltName) {
	// TODO: this should be conditional on the card "wanting" to talk over the beachhead to a server
	// which needs to be a feature, but I'm unclear on how and where to express it.
	const self = this;
	const elt = document.getElementById(eltName);
	const card = new cardClz(this);
	if (this.broker.awaitingServerConnection()) {
		// we can't do anything yet that needs the server, so put a login button in the elt
		const login = document.createElement("button");
		login.innerText = "Log In";
		login.onclick = function() {
			console.log("want to log in");
			window.haveit = function(token, secret) {
				console.log("have credentials:", token, secret);
				env.broker.updateConnection(zinikiServer + '/' + token + '/' + secret);
				elt.innerHTML = '';
				self.startCard(card, elt);
			}
			window.open(zinikiLogin);
		}
		elt.appendChild(login);
	} else {
		this.startCard(card, elt);
	}
	return card;
}

FLContext.prototype.startCard = function(card, elt) {
	card._renderInto(cx, elt);
	var lc = this.findContractOnCard(card, "Lifecycle");
	if (lc && lc.init) {
		var msgs = lc.init(this);
		this.env.queueMessages(this, msgs);
	}
	if (lc && lc.ready) {
		var msgs = lc.ready(this);
		this.env.queueMessages(this, msgs);
	}
	return card;
}

FLContext.prototype.findContractOnCard = function(card, ctr) {
	for (var ce in Object.getOwnPropertyDescriptors(card._contracts)) {
		if (card._contracts[ce][ctr])
			return card._contracts[ce][ctr];
	}
}

FLContext.prototype.needsUpdate = function(card) {
	if (typeof this.updateCards === 'undefined')
		throw Error("cannot update when not in event loop");
	if (!this.updateCards.includes(card))
		this.updateCards.push(card);
}

FLContext.prototype.storeMock = function(name, value) {
	value = this.full(value);
	if (value instanceof ResponseWithMessages) {
		this.env.queueMessages(this, ResponseWithMessages.messages(this, value));
		// because this is a test operation, we dispatch the messages immediately
		this.env.dispatchMessages(this);
		value = ResponseWithMessages.response(this, value);
	}
	if (value instanceof FLObject) {
		var mock = new MockFLObject(value);
		this.env.mocks[name] = mock;
		this.env.cards.push(mock);
	} else
		this.env.mocks[name] = value;
	return value;
}

FLContext.prototype.mockContract = function(contract) {
	const ret = new MockContract(contract);
	this.broker.register(contract.name(), ret);
	return ret;
}

FLContext.prototype.mockAgent = function(agent) {
	return this.env.mockAgent(this, agent);
}

FLContext.prototype.mockCard = function(name, card) {
	return this.env.mockCard(this, name, card);
}

FLContext.prototype.explodingHandler = function() {
	const ret = new ExplodingIdempotentHandler(this);
	return ret;
}

FLContext.prototype.mockHandler = function(contract) {
	const ret = new MockHandler(contract);
	return ret;
}

FLContext.prototype.newdiv = function(cnt) {
	this.env.newdiv(cnt);
}

// show value or expr depending on whether individual nodes are evaluated or not
FLContext.prototype.show = function(val) {
// HACK !  We should map it into a string repn properly
	return "" + val;
}


const FLCard = function(cx) {
    this._renderTree = null;
    this._containedIn = null;
}

FLCard.prototype._renderInto = function(_cxt, div) {
    this._containedIn = div;
    div.innerHTML = '';
    if (this._template) {
        this._renderTree = {}
        var t = document.getElementById(this._template);
        if (t != null) {
            var cloned = t.content.cloneNode(true);
            var ncid = _cxt.nextDocumentId();
            cloned.firstElementChild.id = ncid;
            this._renderTree['_id'] = ncid;
            div.appendChild(cloned);
            this._updateDisplay(_cxt, this._renderTree);
            this._resizeDisplayElements(_cxt, this._renderTree);
        }
    }
    // attach the default handlers to the card
    if (this._eventHandlers) {
        this._attachHandlers(_cxt, this._renderTree, div, "_", null, 1, this); // unbound ones
    }
}

FLCard.prototype._resizeDisplayElements = function(_cxt, _rt) {
    if (!_rt)
        return;
    var container = document.getElementById(_rt._id);
    var cw = container.clientWidth;
    var ch = container.clientHeight;
    var nodes = document.querySelectorAll("#" + _rt._id + " .flas-sizing");
    for (var i=0;i<nodes.length;i++) {
        var n = nodes[i];
        var cl = n.classList;
        for (var j=0;j<cl.length;j++) {
            var cle = cl[j];
            if (cle.startsWith("flas-sizing-")) {
                this._setSizeOf(_cxt, n, cw, ch, cle.replace("flas-sizing-", ""));
                break;
            }
        }
    }
}

FLCard.prototype._setSizeOf = function(_cxt, img, cw, ch, alg) {
    var parent = img.parentElement;
    if (alg.startsWith("target-center-")) {
        var props = alg.replace("target-center-", "");
        var idx = props.indexOf("-");
        var xp = parseFloat(props.substring(0, idx));
        var yp = parseFloat(props.substring(idx+1));
        var vprat = cw/ch;
        var imgrat = img.width/img.height;
        if (isNaN(imgrat))
            return;
        if (vprat < imgrat) { // portrait
            // 1. Figure the desired height of the image to appear in the container and make that thing.height
            parent.style.height = ch;
            img.style.height = ch;
            parent.style.width = "auto";
            img.style.width = "auto";

            // 2. Figure out the new left
            var newImgWid = ch * imgrat;
            var left = -(newImgWid*xp/100-cw/2);
            if (left + newImgWid < cw) {
                left = cw - newImgWid;
                if (left > 0)
                    left /= 2;
            }
            parent.style.left = left;
        } else { // landscape
            // 1. Figure the desired width of the image to appear in the container and make that thing.width
            parent.style.width = cw;
            img.style.width = cw;
            parent.style.height = "auto";
            img.style.height = "auto";
            parent.style.left = 0;

            // 2. Figure out the new top
            var newImgHt = cw / imgrat;
            var top = -(newImgHt*yp/100-ch/2)
            if (top + newImgHt < ch) {
                top = ch - newImgHt;
                if (top > 0)
                    top /= 2;
            }
            parent.style.top = top;
        }
    } else if (alg.startsWith("min-aspect-")) {
        var props = alg.replace("min-aspect-", "");
        var idx = props.indexOf("-");
        var idx2 = props.indexOf("-", idx+1);
        var idx3 = props.indexOf("-", idx2+1);
        var idx4 = props.indexOf("-", idx3+1);
        var xc = parseFloat(props.substring(0, idx)) * cw / 100;
        var yc = parseFloat(props.substring(idx+1, idx2)) * ch / 100;
        var pct = parseFloat(props.substring(idx2+1, idx3)) / 100;
        var xr = parseFloat(props.substring(idx3+1, idx4));
        var yr = parseFloat(props.substring(idx4+1));
        var xp = cw * pct;
        var yp = ch * pct;
        var mp = Math.min(xp, yp);
        xr = xr * mp;
        yr = yr * mp;
        img.style.width = xr;
        img.style.height = yr;
        img.style.left = xc - xr/2;
        img.style.top = yc - yr/2;
    } else if (alg.startsWith("text-")) {
        var props = alg.replace("text-", "");
        var rs = parseFloat(props) / 100;
        var parent = img.parentElement;
        var ps = Math.min(parent.clientWidth, parent.clientHeight);
        var sz = rs * ps;
        parent.style.fontSize = sz;
    } else {
        _cxt.log("do not know sizing algorithm " + alg);
    }
}


FLCard.prototype._currentDiv = function(cx) {
    if (this._renderTree)
        return document.getElementById(this._renderTree._id);
    else
        return this._containedIn;
}

FLCard.prototype._currentRenderTree = function() {
    return this._renderTree;
}

FLCard.prototype._attachHandlers = function(_cxt, rt, div, key, field, option, source, evconds) {
    const evcs = this._eventHandlers()[key];
    if (evcs) {
        if (rt && rt.handlers) {
            for (var i=0;i<rt.handlers.length;i++) {
                var rh = rt.handlers[i];
                // _cxt.env.logger.log("removing event listener from " + div.id + " for " + rh.hi.event._eventName);
                div.removeEventListener(rh.hi.event._eventName, rh.eh);
            }
            delete rt.handlers;
        }
        for (var ej=0;ej<evcs.length;ej++) {
            var handlerInfo = evcs[ej];
            if (!handlerInfo.slot) {
                if (field)
                    continue;
            } else {
                if (field != handlerInfo.slot)
                    continue;
            }
            if (handlerInfo.option && handlerInfo.option != option)
                continue;
            if (evconds && typeof handlerInfo.cond !== 'undefined') {
                if (!evconds[handlerInfo.cond])
                    continue;
            }            
            var eh = _cxt.attachEventToCard(this, handlerInfo, div, { value: source });
            if (eh && rt) {
                if (!rt.handlers) {
                    rt.handlers = [];
                }
                rt.handlers.push({ hi: handlerInfo, eh: eh });
            }
        }
    }
}

FLCard.prototype._updateContent = function(_cxt, rt, templateName, field, option, source, value, fromField) {
    // In general, everything should already be fully evaluated, but we do allow expressions in templates
    value = _cxt.full(value);
    if (typeof value === 'undefined' || value == null)
        value = '';
    var div = document.getElementById(rt._id);
    const node = div.querySelector("[data-flas-content='" + field + "']");
    if (!node.id) {
        var ncid = _cxt.nextDocumentId();
        node.id = ncid;
        rt[field] = { _id: ncid };
        if (source)
            rt[field].source = source;
        else
            rt[field].source = this;
        rt[field].fromField = fromField;
    }
    node.innerHTML = '';
    node.appendChild(document.createTextNode(value));
    if (this._eventHandlers) {
        this._attachHandlers(_cxt, rt[field], node, templateName, field, option, source);
    }
}

FLCard.prototype._updateImage = function(_cxt, rt, templateName, field, option, source, value, fromField) {
    // In general, everything should already be fully evaluated, but we do allow expressions in templates
    value = _cxt.full(value);
    // it should be an Image object
    if (typeof value === 'undefined' || value == null || !(value instanceof Image))
        value = '';
    else
        value = value.getUri();
    var div = document.getElementById(rt._id);
    const node = div.querySelector("[data-flas-image='" + field + "']");
    if (!node.id) {
        var ncid = _cxt.nextDocumentId();
        node.id = ncid;
        rt[field] = { _id: ncid };
        if (source)
            rt[field].source = source;
        else
            rt[field].source = this;
        rt[field].fromField = fromField;
    }
    node.src = value;
    var self = this;
    node.onload = function(ev) { self._imageLoaded(_cxt); };
}

FLCard.prototype._imageLoaded = function(_cxt) {
    this._resizeDisplayElements(_cxt, this._renderTree);
}

FLCard.prototype._updateFromInputs = function() {
    if (this._renderTree)
        this._updateFromEachInput(this._renderTree);
}

FLCard.prototype._updateFromEachInput = function(rt) {
    if (rt.children) {
        for (var i=0;i<rt.children.length;i++) {
            this._updateFromEachInput(rt.children[i]);
        }
    }
    var props = Object.keys(rt);
    for (var i=0;i<props.length;i++) {
        if (props[i] == "_id")
            continue;
        var sub = rt[props[i]];
        if (!sub._id)
            continue;
        var div = document.getElementById(sub._id);
        if (div.tagName == "INPUT" && div.hasAttribute("type") && (div.getAttribute("type") == "text" || div.getAttribute("type") == "password")) {
            if (sub.fromField) {
                sub.source.state.set(sub.fromField, div.value);
            }
        }
    }
}

FLCard.prototype._updateStyle = function(_cxt, rt, templateName, type, field, option, source, constant, ...rest) {
    var styles = '';
    if (constant)
        styles = _cxt.full(constant);
    var evconds = [];
    for (var i=0;i<rest.length;i+=2) {
        if (_cxt.isTruthy(rest[i])) {
            styles += ' ' + _cxt.full(rest[i+1]);
            evconds.push(true);
        } else {
            evconds.push(false);
        }
    }
    var div = document.getElementById(rt._id);
    var node;
    if (type != null) {
        node = div.querySelector("[data-flas-" + type + "='" + field + "']");
        if (!node.id) {
            var ncid = _cxt.nextDocumentId();
            node.id = ncid;
            rt[field] = { _id: ncid };
        }
    } else
        node = div;
    node.className = styles;
    if (this._eventHandlers) {
        this._attachHandlers(_cxt, rt[field], node, templateName, field, option, source, evconds);
    }
}

FLCard.prototype._updateTemplate = function(_cxt, _renderTree, type, field, fn, templateName, value, _tc) {
    value = _cxt.full(value);
    var div = document.getElementById(_renderTree._id);
    const node = div.querySelector("[data-flas-" + type + "='" + field + "']");
    if (node != null) {
        var crt;
        var create = false;
        if (!node.id) {
            var ncid = _cxt.nextDocumentId();
            node.id = ncid;
            crt = _renderTree[field] = { _id: ncid };
            create = true;
        } else
            crt = _renderTree[field];
        node.innerHTML = '';
        if (!value) // if undefined, we want nothing - even when we get around to updating, so make sure that still blanks it
            return;
        var t = document.getElementById(templateName);
        if (t != null) {
            if (Array.isArray(value)) {
                if (!crt.children) {
                    crt.children = [];
                }
                var card = this;
                this._updateList(node, crt.children, value, {
                    insert: function (rtc, ni, v) {
                        card._addItem(_cxt, rtc, node, ni, t, fn, v, _tc);
                    }
                });
            } else if (value instanceof Crobag) {
                if (!crt.children) {
                    crt.children = [];
                }
                var card = this;
                this._updateCrobag(node, crt.children, value, {
                    insert: function(rtc, ni, v) {
                        card._addItem(_cxt, rtc, node, ni, t, fn, v, _tc);
                    }
                });
            } else {
                if (crt.single) { // updating
                    this._addItem(_cxt, crt.single, node, node.firstElementChild, t, fn, value, _tc);
                } else { // creating
                    var rt = crt.single = {};
                    this._addItem(_cxt, rt, node, null, t, fn, value, _tc);
                }
            }
        } else {
            _cxt.log("there is no template " + templateName);
        }
    } else {
        _cxt.log("there is no '" + type + "' called '" + field + "' in " + _renderTree._id);
    }
}

FLCard.prototype._addItem = function(_cxt, rt, parent, currNode, template, fn, value, _tc) {
    if (!currNode) {
        var div = template.content.cloneNode(true);
        var ncid = _cxt.nextDocumentId();
        currNode = div.firstElementChild;
        currNode.id = ncid;
        rt._id = ncid;
        parent.appendChild(currNode);
    }
    try {
        fn.call(this, _cxt, rt, value, _tc);
        if (this._eventHandlers) {
            this._attachHandlers(_cxt, rt, div, template.id, null, null, value);
        }
    } catch (e) {
        _cxt.log("cannot add item: ", value, e);
    }
}

FLCard.prototype._updateContainer = function(_cxt, _renderTree, field, value, fn) {
    value = _cxt.full(value);
    var div = document.getElementById(_renderTree._id);
    const node = div.querySelector("[data-flas-container='" + field + "']");
    if (!node.id) {
        var ncid = _cxt.nextDocumentId();
        node.id = ncid;
        _renderTree[field] = { _id: ncid, children: [] };
    }
    var crt = _renderTree[field];
    if (!value) {
        node.innerHTML = ''; // clear it out
        crt.children = [];
        return;
    }
    var card = this;
    if (Array.isArray(value)) {
        this._updateList(node, crt.children, value, {
            insert: function(rtc, ni, v) {
                fn.call(card, _cxt, rtc, node, ni, v);
            }
        });
    } else if (value instanceof Crobag) {
        this._updateCrobag(node, crt.children, value, {
            insert: function(rtc, ni, v) {
                fn.call(card, _cxt, rtc, node, ni, v);
            }
        });
    } else {
        // a single element container
        var curr = null;
        if (!crt.single)
            crt.single = {};
        else if (value == crt.single.value) {
            curr = node.firstElementChild;
        } else { // clear it out
            node.innerHTML = '';
            crt.single = {};
        }
        fn.call(card, _cxt, crt.single, node, curr, value);
    }
}

FLCard.prototype._updatePunnet = function(_cxt, _renderTree, field, value, fn) {
    value = _cxt.full(value);
    var div = document.getElementById(_renderTree._id);
    const node = div.querySelector("[data-flas-punnet='" + field + "']");
    if (!node.id) {
        var ncid = _cxt.nextDocumentId();
        node.id = ncid;
        _renderTree[field] = { _id: ncid, children: [] };
    }
    var crt = _renderTree[field];
    if (!value) {
        node.innerHTML = ''; // clear it out
        crt.children = [];
        return;
    } else if (value instanceof FLCard) {
        if (crt.children.length == 1 && crt.children[0].value == value)
            return;
        // clear out all extant children that are not "value"
        crt.children = [];
        node.innerHTML = '';

        // create a new nested element
        var inid = _cxt.nextDocumentId();
        crt.children.push({ value });
        const pe = document.createElement("div");
        pe.setAttribute("id", inid);
        node.appendChild(pe);
        value._renderInto(_cxt, pe);
    } else
        throw new Error("what is this? " + value);
}

FLCard.prototype._updateList = function(parent, rts, values, cb) {
    var sw = this._diffLists(rts, values);
    if (sw === true) {
        for (var i=0;i<values.length;i++) {
        	cb.insert(rts[i], parent.children[i], values[i]);
        }
    } else if (sw.op === 'addtoend') {
        // update the ones that were already there
        for (var i=0;i<rts.length;i++) {
        	cb.insert(rts[i], parent.children[i], values[i]);
        }
        for (var i=rts.length;i<values.length;i++) {
            var e = values[i];
            var rt  = {value: e};
            rts.push(rt);
            cb.insert(rt, null, e);
        }
    } else if (sw.op === 'add') {
        var done = [];
        for (var i=0;i<sw.additions.length;i++) {
            var ai = sw.additions[i];
            var e = ai.value;
            var rt  = {value: e};
            rts.splice(ai.where, 0, rt);
            cb.insert(rt, null, e);
            if (ai.where < parent.childElementCount-1)
                parent.insertBefore(parent.lastElementChild, parent.children[ai.where]);
            done.push(ai.where);
        }
        for (var i=0;i<values.length;i++) {
            if (!done.includes(i))
        	cb.insert(rts[i], parent.children[i], values[i]);
        }
    } else if (sw.op === 'removefromend') {
        rts.splice(values.length);
        while (values.length < parent.childElementCount) {
            parent.lastChild.remove();
        }
        // update the rest
        for (var i=0;i<values.length;i++) {
        	cb.insert(rts[i], parent.children[i], values[i]);
        }
    } else if (sw.op === 'remove') {
        for (var i=0;i<sw.removals.length;i++) {
            var ri = sw.removals[i];
            rts.splice(ri.where, 1);
            parent.children[ri.where].remove();
        }
        // update the rest
        for (var i=0;i<values.length;i++) {
        	cb.insert(rts[i], parent.children[i], values[i]);
        }
    } else if (sw.op === 'disaster') {
        // There are any number of sub-cases here but basically we have a "current" map of value index to field id
        // We detach everything we already have from the parent and save it by node id (and copy off the existing rtc array)
        // We then go through the values and either pull back and update or insert a new value, updating the rtc as we go
        var map = {};
        while (parent.firstElementChild) {
            var nd = parent.removeChild(parent.firstElementChild);
            var rtc = rts.shift();
            map[nd.id] = { nd, rtc };
        }
        console.log("disaster map", sw.mapping, map);
        for (var i=0;i<values.length;i++) {
            if (sw.mapping[i]) { // it was already there
                var tmp = map[sw.mapping[i]];
                parent.appendChild(tmp.nd);
                rts.push(tmp.rtc);
                delete map[sw.mapping[i]];
            } else { // add it
                var e = values[i];
                var rt  = {value: e};
                rts.push(rt);
                cb.insert(rt, null, e);
            }
        }
    } else {
        throw new Error("not handled: " + sw.op);
    }
}

FLCard.prototype._updateCrobag = function(parent, rts, crobag, callback) {
    var scrollInfo = this._figureScrollInfo(parent);
    for (var i=0;i<crobag.size();i++) {
        var e = crobag._entries[i];
        if (i >= rts.length) {
            // if we've reached the end, add at the end ...
            var rt  = {value: e};
            rts.push(rt);
            callback.insert(rt, null, e.val);
        } else if (e.key == rts[i].value.key) {
            // if it matches, update it
            callback.insert(rts[i], parent.children[i], e.val);
        } else if (e.key < rts[i].value.key) {
            // a new entry - insert it here
            var rt  = {value: e};
            rts.splice(i, 0, rt);
            callback.insert(rt, null, e.val);
            parent.insertBefore(parent.lastElementChild, parent.children[i]);
        } else if (e.key > rts[i].value.key) {
            // the current entry appears to no longer be in the crobag - remove it
            var rt = rts[i];
            rts.splice(i, 1);
            document.getElementById(rt._id).remove();
        } else {
            debugger;
        }
    }
    switch (scrollInfo.lockMode) {
    case "bottom": {
        scrollInfo.scroller.scrollTop = scrollInfo.scroller.scrollHeight - scrollInfo.lockOffset;
        break;
    }
    case "top": {
        scrollInfo.scroller.scrollTop = parent.children[0].offsetTop + scrollInfo.lockOffset;
        break;
    }
    case "mid": {
        scrollInfo.scroller.scrollTop = scrollInfo.lockDiv.offsetTop - scrollInfo.lockOffset;
        break;
    }
    }
}

FLCard.prototype._figureScrollInfo = function(parent) {
    var div = parent;
    while (div != document.body) {
        var oy = window.getComputedStyle(div)['overflowY'];
        if (oy == 'scroll' || oy == 'auto')
            break;
        div = div.parentElement;
    }
    var min = div.scrollTop;
    var max = min + div.clientHeight;
    var mid = (min + max) / 2;
    var ret = { lockMode: 'bottom', lockOffset: 0, scroller: div, ht : 0, scrollht: div.scrollHeight, scrollTop: div.scrollTop, viewport: div.clientHeight };
    var nodes = parent.children;
    if (nodes.length == 0) {
        return ret;
    }

    var top = nodes[0];
    var bottom = nodes[nodes.length-1];
    
    // see if it's at the bottom
    if (bottom.offsetTop < max) {
        ret.lockMode = 'bottom';
        ret.lockOffset = ret.scrollht - ret.scrollTop;
    } else if (top.offsetTop + top.offsetHeight > min) {
        ret.lockMode = 'top';
        ret.lockOffset = ret.scrollTop - top.offsetTop;
    } else  {
        for (var i=0;i<nodes.length;i++) {
            if (nodes[i].offsetTop + nodes[i].offsetHeight >= mid) {
                ret.lockMode = 'mid';
                ret.lockDiv = nodes[i];
                ret.lockOffset = nodes[i].offsetTop - ret.scrollTop;
                break;
            }
        }
    }
    return ret;
}

/** This is provided with a list of RenderTree child and a new list of values.
 * We are not given any guarantees about either, but we need to figure out what, if anything has changed.
 * If nothing has changed (at the top level) return true;
 * otherwise, return a hash that contains:
 *  op - the broad-brush action to perform
 *    addtoend - there are new entries to add to the end
 *    add - there are new entries but they may go anywhere (see additions)
 *    removefromend - the final few elements need to be removed
 *    remove - remove the specified entries
 *    disaster - it's a complete disaster but some nodes are recoverable: remove everything but be ready to paste them back
 * additions - for add, a list of position and value for new values in reverse order for easy insertion
 */
FLCard.prototype._diffLists = function(rtc, list) {
    var ret = { additions: [], removals: [], mapping: {} };
    var added = false, removed = false;
    var used = {};
    outer:
    for (var i=0,j=0;i<rtc.length && j<list.length;j++) {
        if (rtc[i].value == list[j]) {
            ret.mapping[j] = rtc[i]._id;
            used[i] = true;
            i++;
        } else {
            // try skipping forward through rtc; if you find it mark it "removed" (the rtc[i] has been removed)
            for (var k=i+1;k<rtc.length;k++) {
                if (list[j] === rtc[k].value) {
                    ret.mapping[j] = rtc[k]._id;
                    used[k] = true;
                    ret.removals.unshift({where: i});
                    i = k+1;
                    removed = true;
                    continue outer;
                }
            }
            // try skipping forward through list; if you find an existing one mark this value "added" (there is no current rtc[i] for it)
            for (var k=j+1;k<list.length;k++) {
                if (list[k] === rtc[i].value) {
                    ret.mapping[k] = rtc[i]._id;
                    ret.additions.unshift({where: i, value: list[j]});
                    added = true;
                    continue outer;
                }
            }
            // the list item has been added and the existing item has been removed
            // it's a disaster so try and find the value backwards if we can
            for (var k=i-1;k>=0;k--) {
                if (used[k])
                    continue;
                if (list[j] == rtc[k].value) {
                    // we found it going backwards
                    ret.mapping[j] = rtc[k]._id;
                    used[k] = true;
                    break;
                }
            }
            added = removed = true;
            i++;
        }
    }
    if ((added || j < list.length) && (removed || i < rtc.length)) {
        ret.op = "disaster";
        while (j < list.length) {
            for (var k=0;k<rtc.length;k++) {
                if (used[k])
                    continue;
                if (rtc[k].value == list[j]) {
                    ret.mapping[j] = rtc[k]._id;
                    used[k] = true;
                    break;
                }
            }
            j++;
        }
    } else if (added) {
        ret.op = "add";
        while (j < list.length) {
            ret.additions.unshift({ where: i++, value: list[j++] });
        }
    } else if (removed) {
        ret.op = "remove";
        while (i < rtc.length) {
            ret.removals.unshift({ where: i++ });
        }
    } else if (list.length > rtc.length) {
        ret.op = "addtoend";
    } else if (list.length < rtc.length) {
        ret.op = "removefromend";
    } else
        return true; // nothing appears to have changed
    return ret;
}



const FLObject = function(cx) {
}
FLObject.prototype._updateTemplate = FLCard.prototype._updateTemplate;
FLObject.prototype._addItem = FLCard.prototype._addItem;
FLObject.prototype._updateContent = FLCard.prototype._updateContent;
FLObject.prototype._updateContainer = FLCard.prototype._updateContainer;
FLObject.prototype._updatePunnet = FLCard.prototype._updatePunnet;
FLObject.prototype._updateStyle = FLCard.prototype._updateStyle;
FLObject.prototype._updateList = FLCard.prototype._updateList;
FLObject.prototype._diffLists = FLCard.prototype._diffLists;
FLObject.prototype._attachHandlers = FLCard.prototype._attachHandlers;
FLObject.prototype._resizeDisplayElements = FLCard.prototype._resizeDisplayElements;



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



const Nil = function() {
}

Nil.eval = function(_cxt) {
	return [];
}

/* istanbul ignore next */
const Cons = function() {
}

// Because we "pretend" to have Cons and Nil but actually have arrays,
// we need to put "head" and "tail" on Array for when they are invoked.
// But we need them to be on Cons for when they are referenced.
Array.prototype._field_head = function(x) {
	return this[0];
}
Array.prototype._field_head.nfargs = function() { return 0; }
Cons.prototype._field_head = Array.prototype._field_head;

Array.prototype._field_tail = function() {
	return this.slice(1);
}
Array.prototype._field_tail.nfargs = function() { return 0; }
Cons.prototype._field_tail = Array.prototype._field_tail;

Cons.eval = function(_cxt, hd, tl) {
	var cp = _cxt.spine(tl);
	if (cp instanceof FLError)
		return cp;
	cp = cp.slice(0);
	cp.splice(0, 0, hd);
	return cp;
}

const AssignItem = function(list, n) {
	this.list = list;
	this.n = n;
}

AssignItem.prototype._field_head = function(_cxt) {
	return this.list[this.n];
}
AssignItem.prototype._field_head.nfargs = function() { return 0; }

AssignItem.prototype.set = function(obj) {
	this.list[this.n] = obj;
}



/* Contracts */

// SlideWindow
SlideWindow = function(_cxt) {
    IdempotentHandler.call(this, _cxt);
    return ;
}
SlideWindow.prototype = new IdempotentHandler();
SlideWindow.prototype.constructor = SlideWindow;

SlideWindow.prototype.name = function() {
    return 'SlideWindow';
}

SlideWindow.prototype.name.nfargs = function() { return -1; }

SlideWindow.prototype._methods = function() {
    const v1 = ['success','failure'];
    return v1;
}

SlideWindow.prototype._methods.nfargs = function() { return -1; }

// CrobagWindow
CrobagWindow = function(_cxt) {
    IdempotentHandler.call(this, _cxt);
    return ;
}
CrobagWindow.prototype = new IdempotentHandler();
CrobagWindow.prototype.constructor = CrobagWindow;

CrobagWindow.prototype.name = function() {
    return 'CrobagWindow';
}
CrobagWindow.prototype.name.nfargs = function() { return -1; }

CrobagWindow.prototype._methods = function() {
    const v1 = ['success','failure','next','done'];
    return v1;
}
CrobagWindow.prototype._methods.nfargs = function() { return -1; }

CrobagWindow.prototype.next = function(_cxt, _key, _value, _ih) {
    return 'interface method for CrobagWindow.next';
}
CrobagWindow.prototype.next.nfargs = function() { return 2; }
  
CrobagWindow.prototype.done = function(_cxt, _ih) {
    return 'interface method for CrobagWindow.done';
}
CrobagWindow.prototype.done.nfargs = function() { return 0; }
 

/* CROBAG entry */
const CroEntry = function(key, val) {
    this.key = key;
    this.val = val;
}

CroEntry.fromWire = function(cx, om, fields) {
    var lt = new ListTraverser(cx, om.state);
    om.marshal(lt, fields["value"]);
    return new CroEntry(fields["key"], lt.ret[0]);
}


/* CROBAG itself */
const Crobag = function(_cxt, _card) {
    FLObject.call(this, _cxt);
    this._card = _card;
    // a crobag doesn't have any actual fields, but ZiWSH wants to pretend it does
    this.state = { dict: {} }; // _cxt.fields();
    this._entries = [];
}

Crobag._ctor_new = function(_cxt, _card) {
    const ret = new Crobag(_cxt, _card);
    return new ResponseWithMessages(_cxt, ret, []);
}
Crobag._ctor_new.nfargs = function() { return 1; }

Crobag.fromWire = function(cx, om, fields) {
    var ret = new Crobag(cx, null);
    var os = fields["entries"];
    if (os.length > 0) {
        var lt = new ListTraverser(cx, om.state);
        for (var i=0;i<os.length;i++) {
            om.marshal(lt, os[i]);
        }
        ret._entries = lt.ret;
    }
    return ret;
}

Crobag.prototype.insert = function(_cxt, key, val) {
    return [CrobagChangeEvent.eval(_cxt, this, "insert", key, null, val)];
}
Crobag.prototype.insert.nfargs = function() { return 1; }

Crobag.prototype.put = function(_cxt, key, val) {
    return [CrobagChangeEvent.eval(_cxt, this, "put", key, null, val)];
}
Crobag.prototype.put.nfargs = function() { return 1; }

Crobag.prototype.upsert = function(_cxt, key, val) {
    return [CrobagChangeEvent.eval(_cxt, this, "upsert", key, null, val)];
}
Crobag.prototype.upsert.nfargs = function() { return 1; }

Crobag.prototype.window = function(_cxt, from, size, handler) {
    return [CrobagWindowEvent.eval(_cxt, this, from, size, handler)];
}
Crobag.prototype.window.nfargs = function() { return 3; }

Crobag.prototype.size = function(_cxt) {
    return this._entries.length;
}
Crobag.prototype.size.nfargs = function() { return 0; }

// internal method called from CCE.dispatch()
Crobag.prototype._change = function(cx, op, newKey, remove, val) {
    if (newKey != null) {
        var e = new CroEntry(newKey, val);
        var done = false;
        for (var i=0;i<this._entries.length;i++) {
            if (this._entries[i].key > newKey) {
                this._entries.splice(i, 0, e);
                done = true;
                break;
            } else if (this._entries[i].key == newKey) {
                if (op == "insert") {
                    continue;
                } else if (op == "put") {
                    // just shove it in
                    this._entries.splice(i, 1, e);
                } else if (op == "upsert") {
                    // update this_.entries[i] with values from e
                }

                done = true;
                break;
            }
        }
        if (!done)
            this._entries.push(e);
    }
}

Crobag.prototype._methods = function() {
    return {
        "insert": Crobag.prototype.insert,
        "put": Crobag.prototype.put,
        "size": Crobag.prototype.size,
        "upsert": Crobag.prototype.upsert,
        "window": Crobag.prototype.window
    };
}

// Events

const CrobagChangeEvent = function() {
}
CrobagChangeEvent.eval = function(_cxt, bag, op, newKey, remove, val) {
    const e = new CrobagChangeEvent();
    e.bag = bag;
    e.op = op;
    e.newKey = newKey;
    e.remove = remove;
    e.val = val;
	return e;
}
CrobagChangeEvent.prototype._compare = function(cx, other) {
	if (other instanceof CrobagChangeEvent) {
		return other.msg == this.msg;
	} else
		return false;
}
CrobagChangeEvent.prototype.dispatch = function(cx) {
    this.bag = cx.full(this.bag);
    if (this.bag instanceof FLError)
        return this.bag;
    this.op = cx.full(this.op);
    if (this.op instanceof FLError)
        return this.op;
    this.newKey = cx.full(this.newKey);
    if (this.newKey instanceof FLError)
        return this.newKey;
    this.remove = cx.full(this.remove);
    if (this.remove instanceof FLError)
        return this.remove;
    this.val = cx.full(this.val);
    if (this.val instanceof FLError)
        return this.val;
    this.bag._change(cx, this.op, this.newKey, this.remove, this.val);
    return [];
}
CrobagChangeEvent.prototype.toString = function() {
	return "CrobagChangeEvent[" + this.from + ":" + this.size + "]";
}

// Note: strictly speaking, I am not sure this event is needed
// I think we could just return the list of "Send" events directly from window
const CrobagWindowEvent = function() {
}
CrobagWindowEvent.eval = function(_cxt, bag, from, size, replyto) {
    const e = new CrobagWindowEvent();
    e.bag = bag;
    e.from = from;
    e.size = size;
    e.replyto = replyto;
	return e;
}
CrobagWindowEvent.prototype._compare = function(cx, other) {
	if (other instanceof CrobagWindowEvent) {
		return other.msg == this.msg;
	} else
		return false;
}
CrobagWindowEvent.prototype.dispatch = function(cx) {
    this.bag = cx.full(this.bag);
    if (this.bag instanceof FLError)
        return this.bag;
    this.from = cx.full(this.from);
    if (this.from instanceof FLError)
        return this.from;
    this.size = cx.full(this.size);
    if (this.size instanceof FLError)
        return this.size;
    this.replyto = cx.full(this.replyto);
    if (this.replyto instanceof FLError)
        return this.replyto;
    var arr = [];
    var k = 0;
    for (var i=0;i<this.bag._entries.length;i++) {
        var e = this.bag._entries[i];
        if (e.key < this.from)
            continue;
        if (k >= this.size)
            break;
        arr.push(Send.eval(cx, this.replyto, "next", [e.key, e.val], null));
    }
    arr.push(Send.eval(cx, this.replyto, "done", [], _ActualSlideHandler.eval(cx, this.crobag)));
    return arr;
}
CrobagWindowEvent.prototype.toString = function() {
	return "CrobagWindowEvent[" + this.from + ":" + this.size + "]";
}

_ActualSlideHandler = function(_cxt, crobag) {
    SlideWindow.call(this, _cxt);
    this.state = _cxt.fields();
    this._card = crobag;
    return;
}
_ActualSlideHandler.prototype = new SlideWindow();
_ActualSlideHandler.prototype.constructor = _ActualSlideHandler;

_ActualSlideHandler.eval = function(_cxt, crobag) {
    const v1 = new _ActualSlideHandler(_cxt, crobag);
    v1.state.set('_type', '_ActualSlideHandler');
    return v1;
}
_ActualSlideHandler.eval.nfargs = function() { return 1; }

_ActualSlideHandler.prototype._card = function() {
    return this._card;
}
_ActualSlideHandler.prototype._card.nfargs = function() { return -1; }
  


const Image = function(_cxt, _uri) {
    FLObject.call(this, _cxt);
    this.state = _cxt.fields();
	this.state.set("uri", _uri);
}

Image._ctor_asset = function(_cxt, _card, _uri) {
    const ret = new Image(_cxt, _uri);
    return new ResponseWithMessages(_cxt, ret, []);
}
Image._ctor_asset.nfargs = function() { return 2; }

Image.prototype.getUri = function() {
	return this.state.get("uri");
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
const TypeOf = function(ty) {
	this.ty = ty;
}
TypeOf.eval = function(_cxt, expr) {
	expr = _cxt.full(expr);
	if (typeof(expr) == 'object')
	  	return new TypeOf(expr.constructor.name);
	else
		return new TypeOf(typeof(expr));
}
TypeOf.prototype._compare = function(_cxt, other) {
	if (other instanceof TypeOf) {
		return this.ty == other.ty;
	} else
		return false;
}
TypeOf.prototype.toString = function() {
	if (this.ty._typename) {
		return this.ty._typename;
	}
	switch (this.ty) {
	case 'number':
		return "Number";
	case 'string':
		return "String";
	case 'TypeOf':
		return 'Type';
	default:
		return this.ty;
	}
}

TypeOf.prototype._towire = function(wf) {
	wf.type = this.toString();
	wf._wireable = 'org.flasck.jvm.builtin.TypeOf';
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

FLBuiltin.unaryMinus = function(_cxt, a) {
	a = _cxt.full(a);
	return -a;
}

FLBuiltin.unaryMinus.nfargs = function() { return 1; }

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

FLBuiltin.mod = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a%b;
}

FLBuiltin.mod.nfargs = function() { return 2; }

FLBuiltin.not = function(_cxt, a) {
	a = _cxt.full(a);
	return !a;
}

FLBuiltin.not.nfargs = function() { return 1; }

FLBuiltin.boolAnd = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return _cxt.isTruthy(a) && _cxt.isTruthy(b);
}

FLBuiltin.boolAnd.nfargs = function() { return 2; }

FLBuiltin.boolOr = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return _cxt.isTruthy(a) || _cxt.isTruthy(b);
}

FLBuiltin.boolOr.nfargs = function() { return 2; }

FLBuiltin.concat = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a + b;
}

FLBuiltin.concat.nfargs = function() { return 2; }

FLBuiltin.nth = function(_cxt, n, list) {
	n = _cxt.full(n);
	if (typeof(n) != 'number')
		return new FLError("no matching case");
	list = _cxt.spine(list);
	if (!Array.isArray(list))
		return new FLError("no matching case");
	if (n < 0 || n >= list.length)
		return new FLError("out of bounds");
	return list[n];
}

FLBuiltin.nth.nfargs = function() { return 2; }

FLBuiltin.item = function(_cxt, n, list) {
	n = _cxt.full(n);
	if (typeof(n) != 'number')
		return new FLError("no matching case");
	list = _cxt.spine(list);
	if (!Array.isArray(list))
		return new FLError("no matching case");
	if (n < 0 || n >= list.length)
		return new FLError("out of bounds");
	return new AssignItem(list, n);
}

FLBuiltin.item.nfargs = function() { return 2; }

FLBuiltin.append = function(_cxt, list, elt) {
	list = _cxt.spine(list);
	if (!Array.isArray(list))
		return new FLError("no matching case");
	var cp = list.slice(0);
	cp.push(elt);
	return cp;
}

FLBuiltin.append.nfargs = function() { return 2; }

FLBuiltin.replace = function(_cxt, list, n, elt) {
	n = _cxt.full(n);
	if (typeof(n) != 'number')
		return new FLError("no matching case");
	list = _cxt.spine(list);
	if (!Array.isArray(list))
		return new FLError("no matching case");
	if (n < 0 || n >= list.length)
		return new FLError("out of bounds");
	var cp = list.slice(0);
	cp[n] = elt;
	return cp;
}

FLBuiltin.replace.nfargs = function() { return 3; }

FLBuiltin.concatLists = function(_cxt, list) {
	list = _cxt.spine(list);
	var ret = [];
	for (var i=0;i<list.length;i++) {
		var li = _cxt.spine(list[i]);
		for (var j=0;j<li.length;j++) {
			ret.push(li[j]);
		}
	}
	return ret;
}
FLBuiltin.concatLists.nfargs = function() { return 1; }

FLBuiltin.take = function(_cxt, quant, list) {
	list = _cxt.spine(list);
	if (list instanceof FLError)
		return list;
	quant = _cxt.full(quant);
	if (quant instanceof FLError)
		return quant;
	if (typeof quant !== 'number')
		return new FLError("no matching case");
	if (list.length <= quant)
		return list;
	return list.slice(0, quant);
}
FLBuiltin.take.nfargs = function() { return 2; }

FLBuiltin.drop = function(_cxt, quant, list) {
	list = _cxt.spine(list);
	if (list instanceof FLError)
		return list;
	quant = _cxt.full(quant);
	if (quant instanceof FLError)
		return quant;
	if (typeof quant !== 'number')
		return new FLError("no matching case");
	return list.slice(quant);
}
FLBuiltin.drop.nfargs = function() { return 2; }

FLBuiltin.concatMany = function(_cxt, rest) {
	var ret = "";
	for (var i=0;i<rest.length;i++) {
		var tmp = _cxt.full(rest[i]);
		if (!tmp)
			continue;
		if (ret.length > 0)
			ret += " ";
		ret += tmp;
	}
	return ret;
}
FLBuiltin.concatMany.nfargs = function() { return 1; }

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

FLBuiltin.greaterEqual = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a>=b;
}

FLBuiltin.greaterEqual.nfargs = function() { return 2; }

FLBuiltin.greaterThan = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a>b;
}

FLBuiltin.greaterThan.nfargs = function() { return 2; }

FLBuiltin.lessEqual = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a<=b;
}

FLBuiltin.lessEqual.nfargs = function() { return 2; }

FLBuiltin.lessThan = function(_cxt, a, b) {
	a = _cxt.full(a);
	b = _cxt.full(b);
	return a<b;
}

FLBuiltin.lessThan.nfargs = function() { return 2; }

FLBuiltin._probe_state = function(_cxt, mock, v) {
	// mock should be a MockCard or MockAgent (or MockObject or something?)
	var sh = _cxt.full(mock);
	if (sh instanceof FLError)
		return sh;
	else if (sh.routes) {
		if (sh.routes[v] === undefined)
			return new FLError("there is no card bound to route var '" + v + "'");
		return sh.routes[v];
	} else if (sh.card) {
		sh = sh.card;
		sh._updateFromInputs();
	} else if (sh.agent)
		sh = sh.agent;
	if (sh.state.dict[v] === undefined)
		return new FLError("No field '" + v + "' in probe_state");
	return sh.state.dict[v];
}

FLBuiltin._probe_state.nfargs = function() { return 2; }

FLBuiltin._underlying = function(_cxt, mock) {
	return mock._underlying(_cxt);
}

FLBuiltin._underlying.nfargs = function() { return 1; }

// Only allowed in unit tests
// Note that this "breaks" functional programming
// given a list of messages, dispatch each of them *JUST ONCE* - don't keep on evaluating
FLBuiltin.dispatch = function(_cxt, msgs) {
	msgs = _cxt.full(msgs);
	if (msgs instanceof FLError)
		return msgs;
	var ret = [];
	var te = _cxt.env;
	te.queueMessages = function(cx, m) {
		ret.push(m);
	}
	_cxt.env.handleMessages(_cxt, msgs);
	return ret;
}
FLBuiltin.dispatch.nfargs = function() { return 1; }

FLBuiltin.show = function(_cxt, val) {
	val = _cxt.full(val);
	return _cxt.show(val);
}
FLBuiltin.show.nfargs = function() { return 1; }

FLBuiltin.expr = function(_cxt, val) {
	return _cxt.show(val);
}
FLBuiltin.expr.nfargs = function() { return 1; }

const MakeHash = function() {
}
MakeHash.eval = function(_cxt, args) {
	throw Error("should not be called - optimize away");
}

const HashPair = function() {
}
HashPair.eval = function(_cxt, args) {
	var ret = new HashPair();
	ret.m = args[0];
	ret.o = args[1];
	return ret;
}

FLBuiltin.hashPair = function(_cxt, key, value) {
	return HashPair.eval(_cxt, [key, value]);
}
FLBuiltin.hashPair.nfargs = function() { return 2; }

FLBuiltin.assoc = function(_cxt, hash, member) {
	hash = _cxt.spine(hash);
	member = _cxt.full(member);
	if (hash[member])
		return hash[member];
	else
		return new FLError("no member " + member);
}
FLBuiltin.assoc.nfargs = function() { return 2; }

FLBuiltin.parseUri = function(_cxt, s) {
	s = _cxt.full(s);
	if (s instanceof FLError)
		return s;
	else if (typeof(s) !== 'string')
		return new FLError("not a string");
	try {
		return new URL(s);
	} catch (e) {
		_cxt.log("error in parsing", s);
		return new FLError(e);
	}
}
FLBuiltin.parseUri.nfargs = function() { return 1; }

FLBuiltin.parseJson = function(_cxt, s) {
	s = _cxt.full(s);
	if (s instanceof FLError)
		return s;
	return JSON.parse(s);
}
FLBuiltin.parseJson.nfargs = function() { return 1; }



const Interval = function(d, ns) {
    this.days = d;
    this.ns = ns;
}

Interval.prototype.asJs = function() {
    return this.days * 86400000 + (this.ns/1000/1000);
}

Interval.prototype._towire = function(wf) {
    wf.days = days;
    wf.ns = ns;
}

const Instant = function(d, ns) {
    this.days = d;
    this.ns = ns;
}

Instant.prototype.asJs = function() {
    return this.days * 86400000 + (this.ns/1000/1000);
}

Instant.prototype._towire = function(wf) {
    wf.days = days;
    wf.ns = ns;
}

FLBuiltin.seconds = function(_cxt, n) {
    n = _cxt.full(n);
	if (n instanceof FLError)
		return n;
	else if (typeof(n) !== 'number')
        return new FLError("not a number");
	return new Interval(Math.floor(n / 86400), (n % 86400) * 1000 * 1000 * 1000);
}
FLBuiltin.seconds.nfargs = function() { return 1; }

FLBuiltin.milliseconds = function(_cxt, n) {
    n = _cxt.full(n);
	if (n instanceof FLError)
		return n;
	else if (typeof(n) !== 'number')
        return new FLError("not a number");
	return new Interval(Math.floor(n / 86400000), (n % 86400000) * 1000 * 1000 * 1000);
}
FLBuiltin.milliseconds.nfargs = function() { return 1; }

FLBuiltin.fromunixdate = function(_cxt, n) {
    n = _cxt.full(n);
	if (n instanceof FLError)
		return n;
	else if (typeof(n) !== 'number')
        return new FLError("not a number");
	return new Instant(Math.floor(n / 86400), (n % 86400) * 1000 * 1000 * 1000);
}
FLBuiltin.fromunixdate.nfargs = function() { return 1; }

FLBuiltin.unixdate = function(_cxt, i) {
    i = _cxt.full(i);
	if (i instanceof FLError)
		return i;
	else if (!(i instanceof Instant))
        return new FLError("not an instant");
    var ds = i.days;
    var secs = i.ns/1000/1000/1000;
    return ds * 86400 + secs;
}
FLBuiltin.unixdate.nfargs = function() { return 1; }

/* Calendar */
const Calendar = function(_cxt, _card) {
    FLObject.call(this, _cxt);
    this._card = _card;
    this.state = _cxt.fields();
}

Calendar._ctor_gregorian = function(_cxt, _card) {
    const ret = new Calendar(_cxt, _card);
    return new ResponseWithMessages(_cxt, ret, []);
}
Calendar._ctor_gregorian.nfargs = function() { return 1; }

Calendar.prototype.isoDateTime = function(_cxt, inst) {
    inst = _cxt.full(inst);
    if (inst instanceof FLError)
        return inst;
    else if (!(inst instanceof Instant))
        return new FLError("not an instant");
    return dateFormat(new Date(inst.asJs()), dateFormat.masks.isoUtcDateTime);
}
Calendar.prototype.isoDateTime.nfargs = function() { return 1; }

Calendar.prototype._parseIsoItem = function(cursor, nd, decimal) {
    if (typeof(nd) == 'undefined') {
        nd = 2;
    }
    if (cursor.pos >= cursor.str.length)
        return 0;
    while (cursor.str[cursor.pos] == ':' || cursor.str[cursor.pos] == '-')
        ;
    if (cursor.pos >= cursor.str.length)
        return 0;
    var ret = 0;
    for (var i=0;i<nd && cursor.pos < cursor.str.length;i++) {
        var c = cursor.str[cursor.pos++];
        if (c < '0' || c > '9')
            break;
        if (decimal) {
            ret = ret + (c-'0') * decimal;
            decimal /= 10;
        } else {
            ret = ret*10 + (c-'0');
        }
    }
    return ret;
}

Calendar.prototype.parseIsoDateTime = function(_cxt, n) {
    n = _cxt.full(n);
	if (n instanceof FLError)
		return n;
	else if (typeof(n) !== 'string')
        return new FLError("not a string");
    var cursor = { str: n, pos: 0 };
    var year = this._parseIsoItem(cursor, 4);
    var month = this._parseIsoItem(cursor);
    var day = this._parseIsoItem(cursor);
    var dt = new Date();
    dt.setUTCFullYear(year);
    dt.setUTCMonth(month-1);
    dt.setUTCDate(day);
    dt.setUTCHours(0);
    dt.setUTCMinutes(0);
    dt.setUTCSeconds(0);
    dt.setUTCMilliseconds(0);
    var days = Math.floor(dt.getTime() / 86400000);
    if (cursor.pos < cursor.str.length && cursor.str[cursor.pos] == 'T')
        cursor.pos++;
    var hour = this._parseIsoItem(cursor);
    var min = this._parseIsoItem(cursor);
    var sec = this._parseIsoItem(cursor);
    var secs = ((hour*60) + min)*60 + sec;
    if (cursor.pos < cursor.str.length && cursor.str[cursor.pos] == '.')
        cursor.pos++;
    var nanos = this._parseIsoItem(cursor, 9, 100000000);
    var tz = cursor.str.substr(cursor.pos);
    // TODO: adjust for TZ
	return new Instant(days, secs * 1000 * 1000 * 1000 + nanos);
}
Calendar.prototype.parseIsoDateTime.nfargs = function() { return 1; }

Calendar.prototype._methods = function() {
    return {
        "isoDateTime": Calendar.prototype.isoDateTime,
        "parseIsoDateTime": Calendar.prototype.parseIsoDateTime
    };
}




const FLEvent = function() {
}

const FLEventSourceTrait = function(elt, source) {
    this.elt = elt;
    this.source = source;
}

FLEvent.prototype._eventSource = function(cx, tih) {
    return this.EventSource.source;
}

FLEvent.prototype._methods = function() {
    return {
        _eventSource: FLEvent.prototype._eventSource
    };
}

const ClickEvent = function() {
}
ClickEvent.prototype = new FLEvent();
ClickEvent.prototype.constructor = ClickEvent;
ClickEvent._eventName = 'click';

ClickEvent.eval = function(cx) {
    return new ClickEvent();
}

ClickEvent.prototype._areYouA = function(cx, name) {
    return name == "ClickEvent" || name == "Event";
}

ClickEvent.prototype._makeJSEvent = function (_cxt, div) {
    const ev = new Event("click", { bubbles: true });
    return ev;
}

ClickEvent.prototype._field_source = function (_cxt, ev) {
    return this.EventSource.source;
}
ClickEvent.prototype._field_source.nfargs = function () {
    return 0;
}

const ScrollTo = function(st) {
    this.st = st;
}
ScrollTo.prototype = new FLEvent();
ScrollTo.prototype.constructor = ScrollTo;
ScrollTo._eventName = 'scrollTo';

ScrollTo.eval = function(cx, st) {
    return new ScrollTo(st);
}

ScrollTo.prototype._areYouA = function(cx, name) {
    return name == "ScrollTo" || name == "Event";
}

ScrollTo.prototype._makeJSEvent = function (_cxt, div) {
    div.scrollTop = this.st;
    const ev = new Event("scroll", { bubbles: true });
    return ev;
}

ScrollTo.prototype._field_source = function (_cxt, ev) {
    return this.EventSource.source;
}
ScrollTo.prototype._field_source.nfargs = function () {
    return 0;
}



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
	this.msg = cx.full(this.msg);
	cx.debugmsg(this.msg);
	return null;
}
Debug.prototype.toString = function() {
	return "Debug[" + this.msg + "]";
}

const Send = function() {
}
Send.eval = function(_cxt, obj, meth, args, handle) {
	const s = new Send();
	s.obj = obj;
	s.meth = meth;
	s.args = args;
	s.handle = handle;
	return s;
}
Send.prototype._full = function(cx) {
	this.obj = cx.full(this.obj);
	this.meth = cx.full(this.meth);
	this.args = cx.full(this.args);
	this.handle = cx.full(this.handle);
}
Send.prototype._compare = function(cx, other) {
	if (other instanceof Send) {
		return cx.compare(this.obj, other.obj) && cx.compare(this.meth, other.meth) && cx.compare(this.args, other.args);
	} else
		return false;
}
Send.prototype.dispatch = function(cx) {
	this._full(cx);
	if (this.obj instanceof ResponseWithMessages) {
		// build an array of messages with the RWM ones first and "me" last
		const ret = ResponseWithMessages.messages(cx, this.obj);
		// TODO: consider args and handle
		ret.push(Send.eval(cx, ResponseWithMessages.response(cx, this.obj), this.meth, this.args, this.handle));
		return ret;
	}
	var args = this.args.slice();
	args.splice(0, 0, cx);
	if (this.handle) {
		args.splice(args.length, 0, this.handle);
	} else {
		args.splice(args.length, 0, new IdempotentHandler());
	}
	var meth = this.obj._methods()[this.meth];
	if (!meth)
		return;
	var ret = meth.apply(this.obj, args);
	// if (this.obj._updateDisplay)
	// 	cx.env.queueMessages(cx, new UpdateDisplay(cx, this.obj));
	// else if (this.obj._card && this.obj._card._updateDisplay)
	// 	cx.env.queueMessages(cx, new UpdateDisplay(cx, this.obj._card));
	return ret;
}
Send.prototype.toString = function() {
	return "Send[" + this.obj + ":" + this.meth + "]";
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
Assign.prototype._full = function(cx) {
	this.obj = cx.full(this.obj);
	this.slot = cx.full(this.slot);
	this.expr = cx.full(this.expr);
}
Assign.prototype._compare = function(cx, other) {
	if (other instanceof Assign) {
		return cx.compare(this.obj, other.obj) && cx.compare(this.slot, other.slot) && cx.compare(this.expr, other.expr);
	} else
		return false;
}
Assign.prototype.dispatch = function(cx) {
	// it's possible that obj is a send or something so consider dispatching it first
	var msgs = [];
	var target = this.obj;
	if (target.dispatch) {
		// TODO: I feel this *could* return a RWM, but it currently doesn't
		var rwm = this.obj.dispatch(cx);
		target = rwm;
	}
	if (this.expr instanceof ResponseWithMessages) {
		msgs.unshift(ResponseWithMessages.messages(cx, this.expr));
		this.expr = ResponseWithMessages.response(cx, this.expr);
	}
	target.state.set(this.slot, this.expr);
	if (this.obj._updateDisplay)
		cx.env.queueMessages(cx, new UpdateDisplay(cx, this.obj));
	else if (this.obj._card && this.obj._card._updateDisplay)
		cx.env.queueMessages(cx, new UpdateDisplay(cx, this.obj._card));
	return msgs;
}
Assign.prototype.toString = function() {
	return "Assign[" + "]";
};

const AssignCons = function() {
}
AssignCons.eval = function(_cxt, obj, expr) {
	const s = new AssignCons();
	s.obj = obj;
	s.expr = expr;
	return s;
}
AssignCons.prototype._full = function(cx) {
	this.obj = cx.full(this.obj);
	this.expr = cx.full(this.expr);
}
AssignCons.prototype._compare = function(cx, other) {
	if (other instanceof AssignCons) {
		return cx.compare(this.obj, other.obj) && cx.compare(this.expr, other.expr);
	} else
		return false;
}
AssignCons.prototype.dispatch = function(cx) {
	// it's possible that obj is a send or something so consider dispatching it first
	var msgs = [];
	var target = this.obj;
	if (target.dispatch) {
		// TODO: I feel this *could* return a RWM, but it currently doesn't
		var rwm = this.obj.dispatch(cx);
		target = rwm;
	}
	if (target instanceof FLError) {
		cx.log(target);
		return;
	}
	if (!(target instanceof AssignItem)) {
		throw Error("No, it needs to be an Item");
	}
	if (this.expr instanceof ResponseWithMessages) {
		msgs.unshift(ResponseWithMessages.messages(cx, this.expr));
		this.expr = ResponseWithMessages.response(cx, this.expr);
	}
	target.set(this.expr);
	return msgs;
}
AssignCons.prototype.toString = function() {
	return "AssignCons[" + "]";
};

const ResponseWithMessages = function(cx, obj, msgs) {
	this.obj = obj;
	this.msgs = msgs;
}
ResponseWithMessages.prototype._full = function(cx) {
	this.obj = cx.full(this.obj);
	this.msgs = cx.full(this.msgs);
}
ResponseWithMessages.response = function(cx, rwm) {
	if (rwm instanceof ResponseWithMessages)
		return rwm.obj;
	else
		return rwm;
}
ResponseWithMessages.messages = function(cx, rwm) {
	if (rwm instanceof ResponseWithMessages)
		return rwm.msgs;
	else
		return null;
}

const UpdateDisplay = function(cx, card) {
	this.card = card;
}
UpdateDisplay.prototype._compare = function(cx, other) {
	if (other instanceof UpdateDisplay) {
		return (this.card == other.card || this.card == null || other.card == null);
	} else
		return false;
}
UpdateDisplay.eval = function(cx) {
	return new UpdateDisplay(cx, null);
}
UpdateDisplay.prototype.dispatch = function(cx) {
	if (this.card._updateDisplay)
		cx.needsUpdate(this.card);
}
UpdateDisplay.prototype.toString = function() {
	return "UpdateDisplay";
}



// Use a seedable random number generator
// see http://prng.di.unimi.it/xoshiro128plusplus.c
function xoshiro128(a, b, c, d) {
    function rotl(x, k) {
        return ((x << k) | (x >> (32 - k)));
    }
    return function() {
        var result = (rotl(a + d, 7) + a);
        var t = (b << 9);
        c ^= a;
        d ^= b;
        b ^= c;
        a ^= d;
        c ^= t;
        d = rotl(d, 11);
        return result & 0x7fffffff;
    }
}

const Random = function(_cxt, _card) {
    FLObject.call(this, _cxt);
    this._card = _card;
    this.state = _cxt.fields();
    this.buffer = []; // needs to be cleared every time we "advance"
}

Random._ctor_seed = function(_cxt, _card, s) {
    const ret = new Random(_cxt, _card);
    var seed = s ^ 0xDEADBEEF;
    ret.generateNext = xoshiro128(0x9E3779B9, 0x243F6A88, 0xB7E15162, seed);
    return new ResponseWithMessages(_cxt, ret, []);
}
Random._ctor_seed.nfargs = function() { return 2; }

Random._ctor_unseeded = function(_cxt, _card) {
    const ret = new Random(_cxt, _card);
    var seed = Math.random()*0xFFFFFFFF;
    ret.generateNext = xoshiro128(0x9E3779B9, 0x243F6A88, 0xB7E15162, seed);
    return new ResponseWithMessages(_cxt, ret, []);
}
Random._ctor_unseeded.nfargs = function() { return 1; }

Random.prototype.next = function(_cxt, quant) {
    while (this.buffer.length < quant)
        this.buffer.push(this.generateNext());
    return this.buffer.slice(0, quant);
}
Random.prototype.next.nfargs = function() { return 1; }

Random.prototype.used = function(_cxt, quant) {
    return Send.eval(_cxt, this, "_used", [quant]);
}
Random.prototype.used.nfargs = function() { return 1; }

Random.prototype._used = function(_cxt, quant) {
    while (quant-- > 0 && this.buffer.length > 0)
        this.buffer.shift();
}
Random.prototype._used.nfargs = function() { return 1; }

Random.prototype._methods = function() {
    return {
        "used": Random.prototype.used,
        "_used": Random.prototype._used
    };
}

/*
 * Date Format 1.2.3
 * (c) 2007-2009 Steven Levithan <stevenlevithan.com>
 * MIT license
 *
 * Includes enhancements by Scott Trenda <scott.trenda.net>
 * and Kris Kowal <cixar.com/~kris.kowal/>
 *
 * Accepts a date, a mask, or a date and a mask.
 * Returns a formatted version of the given date.
 * The date defaults to the current date/time.
 * The mask defaults to dateFormat.masks.default.
 */

var dateFormat = function () {
	var	token = /d{1,4}|m{1,4}|yy(?:yy)?|([HhMsTt])\1?|[LloSZ]|"[^"]*"|'[^']*'/g,
		timezone = /\b(?:[PMCEA][SDP]T|(?:Pacific|Mountain|Central|Eastern|Atlantic) (?:Standard|Daylight|Prevailing) Time|(?:GMT|UTC)(?:[-+]\d{4})?)\b/g,
		timezoneClip = /[^-+\dA-Z]/g,
		pad = function (val, len) {
			val = String(val);
			len = len || 2;
			while (val.length < len) val = "0" + val;
			return val;
		};

	// Regexes and supporting functions are cached through closure
	return function (date, mask, utc) {
		var dF = dateFormat;

		// You can't provide utc if you skip other args (use the "UTC:" mask prefix)
		if (arguments.length == 1 && Object.prototype.toString.call(date) == "[object String]" && !/\d/.test(date)) {
			mask = date;
			date = undefined;
		}

		// Passing date through Date applies Date.parse, if necessary
		date = date ? new Date(date) : new Date;
		if (isNaN(date)) throw SyntaxError("invalid date");

		mask = String(dF.masks[mask] || mask || dF.masks["default"]);

		// Allow setting the utc argument via the mask
		if (mask.slice(0, 4) == "UTC:") {
			mask = mask.slice(4);
			utc = true;
		}

		var	_ = utc ? "getUTC" : "get",
			d = date[_ + "Date"](),
			D = date[_ + "Day"](),
			m = date[_ + "Month"](),
			y = date[_ + "FullYear"](),
			H = date[_ + "Hours"](),
			M = date[_ + "Minutes"](),
			s = date[_ + "Seconds"](),
			L = date[_ + "Milliseconds"](),
			o = utc ? 0 : date.getTimezoneOffset(),
			flags = {
				d:    d,
				dd:   pad(d),
				ddd:  dF.i18n.dayNames[D],
				dddd: dF.i18n.dayNames[D + 7],
				m:    m + 1,
				mm:   pad(m + 1),
				mmm:  dF.i18n.monthNames[m],
				mmmm: dF.i18n.monthNames[m + 12],
				yy:   String(y).slice(2),
				yyyy: y,
				h:    H % 12 || 12,
				hh:   pad(H % 12 || 12),
				H:    H,
				HH:   pad(H),
				M:    M,
				MM:   pad(M),
				s:    s,
				ss:   pad(s),
				l:    pad(L, 3),
				L:    pad(L > 99 ? Math.round(L / 10) : L),
				t:    H < 12 ? "a"  : "p",
				tt:   H < 12 ? "am" : "pm",
				T:    H < 12 ? "A"  : "P",
				TT:   H < 12 ? "AM" : "PM",
				Z:    utc ? "UTC" : (String(date).match(timezone) || [""]).pop().replace(timezoneClip, ""),
				o:    (o > 0 ? "-" : "+") + pad(Math.floor(Math.abs(o) / 60) * 100 + Math.abs(o) % 60, 4),
				S:    ["th", "st", "nd", "rd"][d % 10 > 3 ? 0 : (d % 100 - d % 10 != 10) * d % 10]
			};

		return mask.replace(token, function ($0) {
			return $0 in flags ? flags[$0] : $0.slice(1, $0.length - 1);
		});
	};
}();

// Some common format strings
dateFormat.masks = {
	"default":      "ddd mmm dd yyyy HH:MM:ss",
	shortDate:      "m/d/yy",
	mediumDate:     "mmm d, yyyy",
	longDate:       "mmmm d, yyyy",
	fullDate:       "dddd, mmmm d, yyyy",
	shortTime:      "h:MM TT",
	mediumTime:     "h:MM:ss TT",
	longTime:       "h:MM:ss TT Z",
	isoDate:        "yyyy-mm-dd",
	isoTime:        "HH:MM:ss",
	isoDateTime:    "yyyy-mm-dd'T'HH:MM:ss",
	isoUtcDateTime: "UTC:yyyy-mm-dd'T'HH:MM:ss'Z'"
};

// Internationalization strings
dateFormat.i18n = {
	dayNames: [
		"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat",
		"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
	],
	monthNames: [
		"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
		"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
	]
};


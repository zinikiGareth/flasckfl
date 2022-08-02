
const JsonBeachhead = function(factory, name, broker, sender) {
    this.factory = factory;
    this.name = name;
    this.broker = broker;
    this.sender = sender;
}

JsonBeachhead.prototype.unmarshalContract = function(contract) {
    var broker = this.broker;
    var sender = this.sender;
    return {
        begin: function(cx, method) {
            // broker.logger.log("hello #1 " + contract);
            return new JsonArgsMarshaller(broker, {action:"invoke", contract, method, args:[]}, sender, new CollectingState(cx));
        }
    };
}

JsonBeachhead.prototype.dispatch = function(cx, json, replyTo) {
    cx.log("dispatching " + json + " on " + this.name + " and will reply to " + replyTo);
    const jo = JSON.parse(json);
    const uow = this.factory.newContext();

    switch (jo.action) {
        case "invoke": {
            return this.invoke(uow, jo, replyTo);
        }
        case "idem": {
            return this.idem(uow, jo, replyTo);
        }
    }
}

JsonBeachhead.prototype.invoke = function(uow, jo, replyTo) {
    const um = this.broker.unmarshalTo(jo.contract);
    const dispatcher = um.begin(uow, jo.method);
    for (var i=0;i<jo.args.length-1;i++) {
        const o = jo.args[i];
        this.handleArg(dispatcher, uow, o);
    }
    dispatcher.handler(uow, this.makeIdempotentHandler(replyTo, jo.args[jo.args.length-1]));
    return dispatcher.dispatch();
}

JsonBeachhead.prototype.handleArg = function(ux, uow, o) {
    if (typeof(o) === 'string')
        ux.string(o);
    else if (typeof(o) === 'number')
        ux.number(o);
    else if (o._cycle) {
        ux.handleCycle(o._cycle);
    } else if (o._wireable) {
        var omw = new OMWrapper(this, uow, ux);
        ux.wireable(omw, uow.fromWire(omw, o));
    } else if (o._clz) {
        var fm;
        if (ux.cx.structNamed(o._clz))
            fm = ux.beginFields(o._clz);
        else
            fm = ux.beginFields(o._type);
        ux.unpack(fm.collectingAs());
        const ks = Object.keys(o);
        for (var k=0;k<ks.length;k++) {
            fm.field(ks[k]);
            this.handleArg(fm, uow, o[ks[k]]);
        }
    } else
        throw Error("not handled: " + JSON.stringify(o));
}

JsonBeachhead.prototype.idem = function(uow, jo, replyTo) {
    const ih = this.broker.currentIdem(jo.idem);
    if (!ih) {
        uow.log("failed to find idem service for", jo.idem, new Error().stack);
        throw new Error("did not find idem " + jo.idem);
    }
    const um = new UnmarshallerDispatcher(null, ih);
    const dispatcher = um.begin(uow, jo.method);
    // uow.log("jo.args =", JSON.stringify(jo.args));
    var cnt = jo.args.length;
    var wantHandler = false;
    if (jo.method != "success" && jo.method != "failure") {
        wantHandler = true;
        cnt--;
    }
    // uow.log("now cnt =", cnt, "and wantHandler =", wantHandler);
    for (var i=0;i<cnt;i++) {
        this.handleArg(dispatcher, uow, jo.args[i]);
    }
    if (wantHandler) {
        const hi = this.makeIdempotentHandler(replyTo, jo.args[cnt-1]);
        // uow.log("hi =", hi);
        dispatcher.handler(uow, hi);
    }
    return dispatcher.dispatch();
}

JsonBeachhead.prototype.makeIdempotentHandler = function(replyTo, ihinfo) {
    var sender = replyTo;
    const ih = new IdempotentHandler();
    ih.success = function(cx) {
        sender.send({action:"idem", method:"success", idem: ihinfo._ihid, args: []});
    }
    ih.failure = function(cx, msg) {
    }
    return ih;
}

const JsonMarshaller = function(broker, sender, collector) {
    this.broker = broker;
    this.sender = sender;
    this.collector = collector;
}

JsonMarshaller.prototype.string = function(s) {
    this.collect(s);
}

JsonMarshaller.prototype.number = function(n) {
    this.collect(n);
}

JsonMarshaller.prototype.boolean = function(b) {
    this.collect(b);
}

JsonMarshaller.prototype.wireable = function(om, w) {
    var c = { _clz: "_wireable", "_wireable": w._clz };
    w._towire(c);
    this.collect(c);
}

JsonMarshaller.prototype.circle = function(o, as) {
    this.collector.circle(o, as);
}

JsonMarshaller.prototype.handleCycle = function(cr) {
    if (this.collector.already(cr)) {
        this.collect({_cycle:this.collector.get(cr)});
        this.broker.logger.log("handled cycle", cr, new Error().stack);
        return true;
    }
    return false;
}

JsonMarshaller.prototype.beginFields = function(cls) {
    const me = {_clz:cls};
    this.collect(me);
    return new JsonFieldsMarshaller(this.broker, me, this.sender, this.collector);
}

JsonMarshaller.prototype.beginList = function(cls) {
    const me = [];
    this.collect(me);
    return new JsonListMarshaller(this.broker, me, this.sender, this.collector);
}

JsonMarshaller.prototype.handler = function(cx, h) {
    var clz, ihid;
    if (h instanceof NamedIdempotentHandler) {
        cx.log("have NIH we want clz for", h);
        clz = h._handler._clz();
        ihid = h._ihid;
    } else {
        clz = h._clz();
        ihid = this.broker.uniqueHandler(h);
    }
    this.obj.args.push({ "_ihclz": clz, "_ihid": ihid });
}

JsonMarshaller.prototype.dispatch = function() {
    // this.broker.logger.log("trying to send a response");
    this.sender.send(JSON.stringify(this.obj));
}

const JsonArgsMarshaller = function(broker, obj, sender, collector) {
    JsonMarshaller.call(this, broker, sender, collector);
    this.obj = obj;
}

JsonArgsMarshaller.prototype = new JsonMarshaller();
JsonArgsMarshaller.prototype.constructor = JsonArgsMarshaller;

JsonArgsMarshaller.prototype.collect = function(o) {
    this.obj.args.push(o);
}

JsonArgsMarshaller.prototype.complete = function() {
}

const JsonFieldsMarshaller = function(broker, obj, sender, collector) {
    JsonMarshaller.call(this, broker, sender, collector);
    this.obj = obj;
    this.cas = collector.nextName();
}

JsonFieldsMarshaller.prototype = new JsonMarshaller();
JsonFieldsMarshaller.prototype.constructor = JsonFieldsMarshaller;

JsonFieldsMarshaller.prototype.collectingAs = function(o) {
    return this.cas;
}

JsonFieldsMarshaller.prototype.field = function(f) {
    this.currentField = f;
}

JsonFieldsMarshaller.prototype.collect = function(o) {
    this.obj[this.currentField] = o;
}

JsonFieldsMarshaller.prototype.complete = function() {
}

const JsonListMarshaller = function(broker, arr, sender, collector) {
    JsonMarshaller.call(this, broker, sender, collector);
    this.arr = arr;
}

JsonListMarshaller.prototype = new JsonMarshaller();
JsonListMarshaller.prototype.constructor = JsonListMarshaller;

JsonListMarshaller.prototype.collect = function(o) {
    this.arr.push(o);
}

JsonListMarshaller.prototype.complete = function() {
}

const OMWrapper = function(bh, cx, ux) {
    this.bh = bh;
    this.cx = cx;
    this.ux = ux;
    this.state = ux.state;
}

OMWrapper.prototype.marshal = function(trav, o) {
    this.bh.handleArg(trav, this.cx, o);
}



var brokerId = 1;
const SimpleBroker = function(logger, factory, contracts) {
    this.logger = logger;
    this.server = null;
    this.factory = factory;
    this.contracts = contracts;
    this.services = {};
    this.nextHandle = 1;
    this.handlers = {};
    this.serviceHandlers = new Map();
    this.name = "jsbroker_" + brokerId++;
    logger.log("created ", this.name /*, new Error().stack */);
};

SimpleBroker.prototype.connectToServer = function(uri) {
    const zwc = new ZiwshWebClient(this.logger, this.factory, uri);
    const bh = new JsonBeachhead(this.factory, uri, this, zwc);
    this.server = bh;
    zwc.attachBeachhead(bh);
    this.logger.log("attached", bh, "to", zwc);
    return zwc;
}

SimpleBroker.prototype.updateConnection = function(uri) {
    this.server.sender.connectTo(uri);
}

SimpleBroker.prototype.awaitingServerConnection = function() {
    return this.server && !this.server.sender.conn;
}

SimpleBroker.prototype.beachhead = function(bh) {
    // this.logger.log("setting beachhead");
    this.server = bh;
}

SimpleBroker.prototype.register = function(clz, svc) {
    this.services[clz] = new UnmarshallerDispatcher(clz, svc);
}

SimpleBroker.prototype.require = function(clz) {
    const ctr = this.contracts[clz];
    if (ctr == null) {
        throw Error("undefined contract " + clz);
    }
    var svc = this.services[clz];
    if (svc == null) {
        if (this.server != null)
            svc = this.server.unmarshalContract(clz);
        else
            svc = new UnmarshallerDispatcher(clz, NoSuchContract.forContract(ctr));
    }
    return new MarshallerProxy(this.logger, ctr, svc).proxy;
}

SimpleBroker.prototype.unmarshalTo = function(clz) {
    var svc = this.services[clz];
    if (svc != null)
        return svc;
    else if (this.server != null) {
        // this.logger.log("unmarshalling on server", clz);
        return this.server.unmarshalContract(clz);
    }
    else
        return NoSuchContract.forContract(clz);
}

SimpleBroker.prototype.uniqueHandler = function(h) {
    const name = "handler_" + (this.nextHandle++);
    this.handlers[name] = h;
    this.logger.log("registered handler name", name, "in", this.name, "have", JSON.stringify(Object.keys(this.handlers)));
    return name;
}

SimpleBroker.prototype.currentIdem = function(h) {
    const ret = this.handlers[h];
    if (!ret) {
        this.logger.log("there is no handler for", h, "in", this.name, "have", JSON.stringify(Object.keys(this.handlers)));
    }
    return ret;
}

SimpleBroker.prototype.serviceFor = function(h, sf) {
    if (!h._ihid) {
        throw new Error("must have an _ihid");
    }
    this.serviceHandlers.set(h._ihid, sf);
}
SimpleBroker.prototype.cancel = function(cx, old) {
    const ret = this.handlers[old];
    if (!ret) {
        this.logger.log("there is no handler for", old);
        return;
    }
    delete this.handlers[old];
    this.logger.log("need to cancel " + ret);
    if (this.serviceHandlers.has(old)) {
        this.serviceHandlers.get(old).cancel(cx);
        this.serviceHandlers.delete(old);
    }
}



const EvalContext = function(env, broker) {
	this.env = env;
	this.broker = broker;
}

EvalContext.prototype.log = function(...args) {
	this.env.logger.log.apply(this.env.logger, args);
}

EvalContext.prototype.debugmsg = function(...args) {
	if (this.env.logger.debugmsg)
		this.env.logger.debugmsg.apply(this.env.logger, args);
	else
		this.log(args);
}

EvalContext.prototype.bindTo = function(to) {
	var ret = this.split();
	return ret;
}

EvalContext.prototype.split = function() {
	var ret = this.env.newContext();
	return ret;
}

EvalContext.prototype.registerContract = function(name, ctr) {
	if (this.broker && !this.broker.contracts[name])
		this.broker.contracts[name] = ctr;
}

EvalContext.prototype.registerStruct = function(name, str) {
	this.env.structs[name] = str;
}

EvalContext.prototype.structNamed = function(name) {
	return this.env.structs[name];
}

EvalContext.prototype.registerObject = function(name, str) {
	this.env.objects[name] = str;
}

EvalContext.prototype.objectNamed = function(name) {
	return this.env.objects[name];
}

EvalContext.prototype.fields = function() {
	return new FieldsContainer(this);
}

EvalContext.prototype.fromWire = function(om, fields) {
	var clz = this.env.objects[fields["_wireable"]];
	if (!clz) {
		throw Error("could not find a registration for " + fields["_wireable"]);
	}
	return clz.fromWire.call(clz, this, om, fields);
}

EvalContext.prototype._bindNamedHandler = function(nh) {
	// Do we need to do anything? This is really to support FLAS, but maybe (some of) that code should come here ...
}

const FieldsContainer = function(cx) {
	this.cx = cx;
	this.dict = {};
}

FieldsContainer.prototype.set = function(fld, val) {
	this.dict[fld] = val;
}

FieldsContainer.prototype.has = function(fld) {
	return !!this.dict[fld];
}

FieldsContainer.prototype.get = function(fld) {
	const ret = this.dict[fld];
	return ret;
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


const IdempotentHandler = function() {
};

IdempotentHandler.prototype._clz = function() {
    return "org.ziniki.ziwsh.intf.IdempotentHandler";
}

IdempotentHandler.prototype.success = function(cx) {
};

IdempotentHandler.prototype.failure = function(cx, msg) {
};

IdempotentHandler.prototype._methods = function() {
    return [ "success", "failure" ];
}

const LoggingIdempotentHandler = function() {
};

LoggingIdempotentHandler.prototype = new IdempotentHandler();
LoggingIdempotentHandler.prototype.constructor = LoggingIdempotentHandler;

LoggingIdempotentHandler.prototype.success = function(cx) {
    cx.log("success");
};

LoggingIdempotentHandler.prototype.failure = function(cx, msg) {
    cx.log("failure: " + msg);
};

const NamedIdempotentHandler = function(handler, name) {
    this._handler = handler;
    this._name = name;
}



const MarshallerProxy = function(logger, ctr, svc) {
    this.logger = logger;
    this.svc = svc;
    this.proxy = proxy(logger, ctr, this);
}

MarshallerProxy.prototype.invoke = function(meth, args) {
    const cx = args[0];
    try {
        const ux = this.svc.begin(cx, meth);
        new ArgListMarshaller(this.logger, false, true).marshal(cx, ux, args);
        return ux.dispatch();
    } catch (e) {
        cx.log("error during marshalling", e);
        cx.log("error reported at", e.stack);
        throw e;
    }
}

const ArgListMarshaller = function(logger, includeFirst, includeLast) {
    this.logger = logger;
    this.from = includeFirst?0:1;
    this.skipEnd = includeLast?0:1;
}

ArgListMarshaller.prototype.marshal = function(cx, m, args) {
    const om = new ObjectMarshaller(this.logger, m);
    for (var i=this.from;i<args.length-this.skipEnd;i++) {
        om.marshal(cx, args[i]);
    }
}

const ObjectMarshaller = function(logger, top) {
    this.logger = logger;
    this.top = top;
}

ObjectMarshaller.prototype.marshal = function(cx, o) {
    this.recursiveMarshal(cx, this.top, o);
}

ObjectMarshaller.prototype.recursiveMarshal = function(cx, ux, o) {
    // cx.log("o =", o, new Error().stack);
    if (o._throw && o._throw()) {
        cx.log("throwing because object has _throw");
        throw o;
    }
    else if (ux.handleCycle(o))
        ;
    else if (typeof o === "string")
        ux.string(o);
    else if (o instanceof URL)
        ux.url(o);
    else if (typeof o === "number")
        ux.number(o);
    else if (typeof o === "boolean")
        ux.boolean(o);
    else if (typeof o === "object") {
        if (o instanceof IdempotentHandler) {
            var ihid = cx.broker.uniqueHandler(o);
            // we want the prototype of the interface implemented by o
            // Sometimes, this may be the class itself, or it may be the superclass
            var intf;
            if (o._clz && o._methods) {
                intf = o;
                cx.log("identified _clz as", intf._clz());
            } else {
                intf = Object.getPrototypeOf(o);
                var sintf = Object.getPrototypeOf(intf);
                cx.log("have IH with", intf, "and", sintf);
                if (typeof(sintf._methods) !== 'undefined') {
                    intf = sintf;
                }
            }
            var h = new NamedIdempotentHandler(proxy(cx, intf, this.makeHandlerInvoker(cx, ihid)));
            h._ihid = ihid;
            ux.handler(cx, h);
        } else if (o instanceof NamedIdempotentHandler) {
            cx.log("marshalling NIH with", o._ihid, "and", o._name, "and", o._handler.constructor);
            var ihid = cx.broker.uniqueHandler(o._handler);
            cx.log("concluded that ihid should be", ihid);
            o._ihid = ihid;
            cx._bindNamedHandler(o);
            // we want the prototype of the interface implemented by o._handler
            // calling getPrototypeOf gets you the *class* of handler, you then call it again to get the interface
            var intf;
            if (o._handler._clz && o._handler._methods) {
                intf = o._handler;
                cx.log("identified NIH _clz as", intf._clz());
            } else 
            {
                intf = Object.getPrototypeOf(o._handler);
                var sintf = Object.getPrototypeOf(intf);
                cx.log("have NIH with", intf, "and", sintf);
                if (sintf && typeof(sintf._methods) !== 'undefined') {
                    intf = sintf;
                }
            }
            var h = new NamedIdempotentHandler(proxy(cx, intf, this.makeHandlerInvoker(cx, ihid)));
            h._ihid = ihid;
            ux.handler(cx, h);
        } else if (o.state instanceof FieldsContainer) {
            this.handleStruct(cx, ux, o);
        } else if (Array.isArray(o)) {
            this.handleArray(cx, ux, o);
        } else if (o._towire) {
            ux.wireable(this, o);
        } else {
            try {
                cx.log("o =", JSON.stringify(o));
            } catch (e) {
                cx.log("could not stringify", o);
            }
            cx.log("o.state = ", o.state);
            cx.log("cannot handle object with constructor " + o.constructor.name, new Error().stack);
        }
    } else {
        cx.log("typeof o =", typeof o);
        try {
            cx.log("o =", JSON.stringify(o));
        } catch (e) {
            cx.log("could not stringify", o);
        }
        throw Error("cannot handle " + typeof o);
    }
}

ObjectMarshaller.prototype.handleStruct = function(cx, ux, o) {
    const fc = o.state;
    // this.logger.log("at", new Error().stack);
    if (!fc.has("_type")) {
        throw new Error("No _type defined in " + fc);
    }
    const fm = ux.beginFields(fc.get("_type"));
    ux.circle(o, fm.collectingAs());
    const ks = Object.keys(fc.dict);
    for (var k=0;k<ks.length;k++) {
        fm.field(ks[k]);
        this.recursiveMarshal(cx, fm, fc.dict[ks[k]]);
    }
    fm.complete();
}

ObjectMarshaller.prototype.handleArray = function(cx, ux, l) {
    const ul = ux.beginList();
    for (var k=0;k<l.length;k++) {
        this.recursiveMarshal(cx, ul, l[k]);
    }
    ul.complete();
}

ObjectMarshaller.prototype.makeHandlerInvoker = function(cx, ihid) {
    var broker = cx.broker;
    var env = cx.env;
    var handler = new Object();
    // cx.log("MAKING HANDLER INVOKER");
    handler.invoke = function(name, args) {
        var uow = env.newContext();
        const ih = broker.currentIdem(ihid);
        if (!ih) {
            uow.log("failed to find idem handler for", ihid, new Error().stack);
            // throw new Error("NOHDLR\n  did not find idem " + ihid);
            return; // quietly ignore it ...    
        }
        // cx.log("ihid invoker for", name, "with", args);
        const um = new UnmarshallerDispatcher(null, ih);
        const ux = um.begin(uow, name);
        new ArgListMarshaller(this.logger, false, true).marshal(cx, ux, args);
        return ux.dispatch();
    }
    return handler;
}


const NoSuchContract = function(ctr) {
    this.ctr = ctr;
}

const isntThere = function(ctr, meth) {
	return function(cx, ...rest) {
		cx.log("no such contract for", ctr.name(), meth);
		const ih = rest[rest.length-1];
		const msg = "There is no service for " + ctr.name() + ":" + meth;
		throw Error(msg);
	}
}
NoSuchContract.forContract = function(ctr) {
	const nsc = new NoSuchContract(ctr);
	const ms = ctr._methods();
	const meths = {};
	for (var ni=0; ni<ms.length; ni++) {
		var meth = ms[ni];
		meths[meth] = nsc[meth] = isntThere(ctr, meth);
	}
	nsc._methods = function() {
		return meths;
	}
    return nsc;
}



/** Create a proxy of a contract interface
 *  This may also apply to other things, but that's all I care about
 *  We need a:
 *    cx - a context (currently unused)
 *    ctr - an interface object with a _methods() call
 *    handler - a class with a method defined called "invoke" which takes a method name and a list of arguments
 */

const proxy = function(cx, intf, handler) {
    var keys = intf._methods();
    if (!Array.isArray(keys)) { // it's an object for which we want the keys
        keys = Object.keys(keys);
    }
    if (intf instanceof IdempotentHandler) {
        keys.push("success");
        keys.push("failure");
    }
    const proxied = { _owner: handler };
    const methods = {};
    for (var i=0;i<keys.length;i++) {
    	const meth = keys[i];
	    methods[meth] = proxied[meth] = proxyMeth(meth, handler);
    }
    proxied._methods = function() {
    	return methods;
    }
    if (intf._clz) {
        var clz = intf._clz();
        proxied._clz = function() {
            return clz;
        }
    }
    // This feels like functionality bleed from FLAS
    // TODO: clean it up
    if (intf._card) {
        proxied._card = intf._card;
    }
    return proxied;
}

const proxyMeth = function(meth, handler) {
	return function(...args) {
        const ret = handler['invoke'].call(handler, meth, args);
        return ret;
    }
}

const proxy1 = function(cx, underlying, methods, handler) {
    const myhandler = {
        get: function(target, ps, receiver) {
            const prop = String(ps);
            if (methods.includes(prop)) {
                const fn = function(...args) {
                    const ret = handler['invoke'].call(handler, prop, args);
                    return ret;
                }
                return fn;
            } else if (target[prop]) {
                return target[prop];
            } else {
                return function() {
                    return "-no-such-method-";
                };
            }
        }
    };
    var proxied = new Proxy(underlying, myhandler);
    return proxied;
}



const Unmarshaller = function() {
}

const UnmarshallerDispatcher = function(ctr, svc) {
    this.svc = svc;
    Unmarshaller.call(this);
}

UnmarshallerDispatcher.prototype = new Unmarshaller();
UnmarshallerDispatcher.prototype.constructor = UnmarshallerDispatcher;

UnmarshallerDispatcher.prototype.begin = function(cx, method) {
    return new DispatcherTraverser(this.svc, method, cx, new CollectingState(cx));
}

UnmarshallerDispatcher.prototype.toString = function() {
    return "ProxyFor[UnmarshallerDispatcher]";
}

const CollectingTraverser = function(cx, collector) {
    this.cx = cx;
    this.collector = collector;
}

CollectingTraverser.prototype.string = function(s) {
    this.collect(s);
}

CollectingTraverser.prototype.url = function(u) {
    this.collect(u);
}

CollectingTraverser.prototype.number = function(n) {
    this.collect(n);
}

CollectingTraverser.prototype.boolean = function(b) {
    this.collect(b);
}

CollectingTraverser.prototype.wireable = function(om, w) {
    this.collect(w);
}

CollectingTraverser.prototype.handleCycle = function(cr) {
    const already = this.collector.already(cr);
    if (already) {
        this.collect(this.collector.get(cr));
    }
    return already;
}

CollectingTraverser.prototype.circle = function(o, as) {
    this.collector.circle(o, as);
}

CollectingTraverser.prototype.unpack = function(collectingAs) {
    this.collector.circle(this.collector.nextName(), collectingAs);
}

CollectingTraverser.prototype.beginFields = function(clz) {
    const ft = new FieldsTraverser(this.cx, this.collector, clz);
    this.collect(ft.creation);
    return ft;
}

CollectingTraverser.prototype.beginList = function() {
    const lt = new ListTraverser(this.cx, this.collector);
    this.collect(lt.ret);
    return lt;
}

CollectingTraverser.prototype.handler = function(cx, h) {
    this.collect(h);
}

const FieldsTraverser = function(cx, collector, clz) {
    CollectingTraverser.call(this, cx, collector);
    var czz = cx.structNamed(clz);
    if (!czz)
        czz = cx.objectNamed(clz);
    if (!czz)
        throw new Error("Could not find a definition for " + clz);
    // cx.log("creating " + clz + ": " + czz);
    this.creation = new czz(cx);
    this.creation.state._wrapper = this.creation;
}

FieldsTraverser.prototype = new CollectingTraverser();
FieldsTraverser.prototype.constructor = FieldsTraverser;

FieldsTraverser.prototype.field = function(f) {
    this.currentField = f;
}

FieldsTraverser.prototype.collect = function(o) {
    this.creation.state.dict[this.currentField] = o;
    delete this.currentField;
}

FieldsTraverser.prototype.collectingAs = function() {
    return this.creation;
}

FieldsTraverser.prototype.complete = function() {
}

const ListTraverser = function(cx, collector) {
    CollectingTraverser.call(this, cx, collector);
    this.ret = [];
}

ListTraverser.prototype = new CollectingTraverser();
ListTraverser.prototype.constructor = ListTraverser;

ListTraverser.prototype.collect = function(o) {
    this.ret.push(o);
}

ListTraverser.prototype.complete = function() {
}

const UnmarshalTraverser = function(cx, collector) {
    ListTraverser.call(this, cx, collector);
    this.ret.push(cx);
}

UnmarshalTraverser.prototype = new ListTraverser();
UnmarshalTraverser.prototype.constructor = UnmarshalTraverser;

const DispatcherTraverser = function(svc, method, cx, collector) {
    // cx.log("have cx", cx.constructor, new Error().stack);
    // cx.log("have service", svc, svc instanceof NamedIdempotentHandler, new Error().stack);
    UnmarshalTraverser.call(this, cx, collector);
    if (svc instanceof NamedIdempotentHandler) {
        svc = svc._handler;
        // cx.log("... now have service", svc, svc instanceof NamedIdempotentHandler);
    }
    if (!svc[method])
        throw Error("no method '" + method + "': have " + JSON.stringify(Object.keys(svc)));
    this.svc = svc;
    this.method = method;
}

DispatcherTraverser.prototype = new UnmarshalTraverser();
DispatcherTraverser.prototype.constructor = DispatcherTraverser;

DispatcherTraverser.prototype.dispatch = function() {
    var cx = this.ret[0];
    var ih = this.ret[this.ret.length-1];
    // cx.log(new Error().stack);
    // cx.log("js cx = " + cx + " " + JSON.stringify(Object.keys(cx)));
    // cx.log("env = " + Object.keys(cx.env));
    this.ret[0] = cx = cx.env.newContext().bindTo(this.svc);
    try {
        if (ih instanceof NamedIdempotentHandler) {
            cx.log("have NIH in dispatch");
        }
        var rets = this.svc[this.method].apply(this.svc, this.ret);
        // I don't think this matches the semantics of when we want success to be called
        // I think we should add an event to the end of the list of actions
        // But have test cases to prove that and hold that
        // ih.success(this.cx);
    } catch (e) {
        cx.log("caught exception and reporting failure", e.toString());
        if (ih instanceof NamedIdempotentHandler) {
            ih = ih._handler;
        }
        
        ih.failure(cx, e.message);
    }
    return rets;
}

const CollectingState = function(cx) {
    this.cx = cx;
    this.containing = [];
    this.named = {};
    this.next = 0;
}

CollectingState.prototype.nextName = function() {
    return "c" + (this.next++);
}

CollectingState.prototype.circle = function(o, as) {
    this.containing.push({obj: o, stored: as});
    if (typeof o === "string")
        this.named[o] = as;
}

CollectingState.prototype.already = function(cr) {
    if (this.named[cr])
        return true;
    for (var i=0;i<this.containing.length;i++) {
        if (cr === this.containing[i].obj)
            return true;
    }
    return false;
}

CollectingState.prototype.get = function(cr) {
    if (this.named[cr])
        return this.named[cr];
    for (var i=0;i<this.containing.length;i++) {
        if (cr === this.containing[i].obj)
            return this.containing[i].stored;
    }
    throw Error("no key" + cr);
}


// TODO: I think I want to further remove the websocket abstraction from here
// so that I can test all the backlog/reconnect logic
// It needs to pass in a factory instead of URI, I think ...

const ZiwshWebClient = function(logger, factory, uri) {
    this.logger = logger;
    this.factory = factory;
    if (uri) {
        this.connectTo(uri);
    }
    logger.log("created ZWC");
}

ZiwshWebClient.prototype.connectTo = function(uri) {
    const zwc = this;
    this.logger.log("connecting to URI " + uri);
    this.conn = new WebSocket(uri);
    this.logger.log("have ws", this.conn);
    this.backlog = [];
    this.conn.addEventListener("error", (ev) => {
        zwc.logger.log("an error occurred");
    });
    this.conn.addEventListener("open", () => {
        zwc.logger.log("opened with backlog", this.backlog.length);
        while (this.backlog.length > 0) {
            const json = zwc.backlog.pop();
            zwc.logger.log("sending", json);
            zwc.conn.send(json);
        }
        zwc.logger.log("cleared backlog");
    });
    this.conn.addEventListener("message", (ev) => {
        // zwc.logger.log("received a message", ev.data);
        // zwc.logger.log("dispatching to", this.bh);
        const cx = zwc.factory.newContext();
        var actions = this.bh.dispatch(cx, ev.data, this);
        if (zwc.factory.queueMessages) {
            zwc.factory.queueMessages(cx, actions);
        }
    });
}
ZiwshWebClient.prototype.attachBeachhead = function(bh) {
    this.bh = bh;
}

ZiwshWebClient.prototype.send = function(json) {
    this.logger.log("want to send " + json + " to " + this.bh.name + (this.conn ? (this.conn.readyState == 1 ? "" : "; not ready; state = " + this.conn.readyState) : " not connected" ));
    if (this.conn && this.conn.readyState == 1)
        this.conn.send(json);
    else
        this.backlog.push(json);
}


/* istanbul ignore else */ 
if (typeof(module) !== 'undefined')
	module.exports = { EvalContext, FieldsContainer, IdempotentHandler, NamedIdempotentHandler, JsonBeachhead, SimpleBroker };
else {
	window.EvalContext = EvalContext;
	window.FieldsContainer = FieldsContainer;
	window.IdempotentHandler = this.IdempotentHandler;
	window.NamedIdempotentHandler = this.NamedIdempotentHandler;
	window.JsonBeachhead = JsonBeachhead;
	window.SimpleBroker = SimpleBroker;
}
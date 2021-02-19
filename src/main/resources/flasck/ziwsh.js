
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
        this.handleArg(dispatcher, o);
    }
    dispatcher.handler(this.makeIdempotentHandler(replyTo, jo.args[jo.args.length-1]));
    return dispatcher.dispatch();
}

JsonBeachhead.prototype.handleArg = function(ux, o) {
    if (typeof(o) === 'string')
        ux.string(o);
    else if (typeof(o) === 'number')
        ux.number(o);
    else if (o._cycle) {
        ux.handleCycle(o._cycle);
    } else if (o._wireable) {
        debugger;
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
            this.handleArg(fm, o[ks[k]]);
        }
    } else
        throw Error("not handled: " + JSON.stringify(o));
}

JsonBeachhead.prototype.idem = function(uow, jo, replyTo) {
    const ih = this.broker.currentIdem(jo.idem);
    const um = new UnmarshallerDispatcher(null, ih);
    const dispatcher = um.begin(uow, jo.method);
    uow.log("jo.args =", jo.args);
    var cnt = jo.args.length;
    var wantHandler = false;
    if (jo.method != "success" && jo.method != "failure") {
        wantHandler = true;
        cnt--;
    }
    for (var i=0;i<cnt;i++) {
        this.handleArg(dispatcher, jo.args[i]);
    }
    if (wantHandler)
        dispatcher.handler(this.makeIdempotentHandler(replyTo, jo.args[cnt-1]));
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

JsonMarshaller.prototype.wireable = function(w) {
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

JsonMarshaller.prototype.handler = function(h) {
    this.obj.args.push({"_ihclz":h._clz(), "_ihid": this.broker.uniqueHandler(h)});
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



const SimpleBroker = function(logger, factory, contracts) {
    this.logger = logger;
    this.server = null;
    this.factory = factory;
    this.contracts = contracts;
    this.services = {};
    this.nextHandle = 1;
    this.handlers = {};
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
    return name;
}

SimpleBroker.prototype.currentIdem = function(h) {
    const ret = this.handlers[h];
    if (!ret) {
        this.logger.log("there is no handler for", h);
    }
    return ret;
}



const EvalContext = function(env, broker) {
	this.env = env;
	this.broker = broker;
}

EvalContext.prototype.log = function(...args) {
	this.env.logger.log.apply(this.env.logger, args);
}

EvalContext.prototype.debugmsg = function(...args) {
	this.env.logger.debugmsg.apply(this.env.logger, args);
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



const MarshallerProxy = function(logger, ctr, svc) {
    this.logger = logger;
    this.svc = svc;
    this.proxy = proxy(logger, ctr, this);
}

MarshallerProxy.prototype.invoke = function(meth, args) {
    const cx = args[0];
    const ux = this.svc.begin(cx, meth);
    new ArgListMarshaller(this.logger, false, true).marshal(ux, args);
    return ux.dispatch();
}

const ArgListMarshaller = function(logger, includeFirst, includeLast) {
    this.logger = logger;
    this.from = includeFirst?0:1;
    this.skipEnd = includeLast?0:1;
}

ArgListMarshaller.prototype.marshal = function(m, args) {
    const om = new ObjectMarshaller(this.logger, m);
    for (var i=this.from;i<args.length-this.skipEnd;i++) {
        om.marshal(args[i]);
    }
}

const ObjectMarshaller = function(logger, top) {
    this.logger = logger;
    this.top = top;
}

ObjectMarshaller.prototype.marshal = function(o) {
    this.recursiveMarshal(this.top, o);
}

ObjectMarshaller.prototype.recursiveMarshal = function(ux, o) {
    if (ux.handleCycle(o))
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
        if (o instanceof IdempotentHandler)
            ux.handler(o);
        else if (o.state instanceof FieldsContainer) {
            this.handleStruct(ux, o);
        } else if (Array.isArray(o)) {
            this.handleArray(ux, o);
        } else if (o._towire) {
            ux.wireable(o);
        } else {
            this.logger.log("o =", JSON.stringify(o));
            this.logger.log("o.state = ", o.state);
            try {
                throw Error("cannot handle object with constructor " + o.constructor.name);
            } catch (e) {
                this.logger.log(e.stack);
                throw e;
            }
        }
    } else {
        this.logger.log("typeof o =", typeof o);
        this.logger.log("o =", JSON.stringify(o));
        throw Error("cannot handle " + typeof o);
    }
}

ObjectMarshaller.prototype.handleStruct = function(ux, o) {
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
        this.recursiveMarshal(fm, fc.dict[ks[k]]);
    }
    fm.complete();
}

ObjectMarshaller.prototype.handleArray = function(ux, l) {
    const ul = ux.beginList();
    for (var k=0;k<l.length;k++) {
        this.recursiveMarshal(ul, l[k]);
    }
    ul.complete();
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
 *    cx - a context (mainly used for logging)
 *    ctr - the *NAME* of the interface which must be defined in window.contracts
 *    handler - a class with a method defined called "invoke" which takes a method name and a list of arguments
 */

const proxy = function(cx, intf, handler) {
    const keys = intf._methods();
    const proxied = { _owner: handler };
    const methods = {};
    for (var i=0;i<keys.length;i++) {
    	const meth = keys[i];
	    methods[meth] = proxied[meth] = proxyMeth(meth, handler);
    }
    proxied._methods = function() {
    	return methods;
    }
    return proxied;
}

const proxyMeth = function(meth, handler) {
	return function(...args) {
		const cx = args[0];
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

CollectingTraverser.prototype.wireable = function(w) {
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

CollectingTraverser.prototype.handler = function(h) {
    this.collect(h);
}

const FieldsTraverser = function(cx, collector, clz) {
    CollectingTraverser.call(this, cx, collector);
    const czz = cx.structNamed(clz);
    cx.log("creating " + clz + ": " + czz);
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
    UnmarshalTraverser.call(this, cx, collector);
    if (!svc[method])
        throw Error("no method '" + method + "'");
    this.svc = svc;
    this.method = method;
}

DispatcherTraverser.prototype = new UnmarshalTraverser();
DispatcherTraverser.prototype.constructor = DispatcherTraverser;

DispatcherTraverser.prototype.dispatch = function() {
    const ih = this.ret[this.ret.length-1];
    try {
        var rets = this.svc[this.method].apply(this.svc, this.ret);
        // I don't think this matches the semantics of when we want success to be called
        // I think we should add an event to the end of the list of actions
        // But have test cases to prove that and hold that
        // ih.success(this.cx);
    } catch (e) {
        ih.failure(this.cx, e.message);
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
        zwc.logger.log("received a message", ev.data);
        zwc.logger.log("dispatching to", this.bh);
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
    this.logger.log("want to send " + json + " to " + this.bh.name + (this.conn ? (" state = " + this.conn.readyState) : " not connected" ));
    if (this.conn && this.conn.readyState == 1)
        this.conn.send(json);
    else
        this.backlog.push(json);
}


/* istanbul ignore else */ 
if (typeof(module) !== 'undefined')
	module.exports = { EvalContext, FieldsContainer, IdempotentHandler, JsonBeachhead, SimpleBroker };
else {
	window.EvalContext = EvalContext;
	window.FieldsContainer = FieldsContainer;
	window.IdempotentHandler = this.IdempotentHandler;
	window.JsonBeachhead = JsonBeachhead;
	window.SimpleBroker = SimpleBroker;
}

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
            return new JsonMarshaller(broker, {action:"invoke", contract, method, args:[]}, sender);
        }
    };
}

JsonBeachhead.prototype.dispatch = function(cx, json, replyTo) {
    cx.log("dispatching " + json + " on " + this.name + " and will reply to " + replyTo);
    const jo = JSON.parse(json);
    const uow = this.factory.newContext();

    switch (jo.action) {
        case "invoke": {
            this.invoke(uow, jo, replyTo);
            break;
        }
        case "idem": {
            this.idem(uow, jo, replyTo);
        }
    }
}

JsonBeachhead.prototype.invoke = function(uow, jo, replyTo) {
    const um = this.broker.unmarshalTo(jo.contract);
    const dispatcher = um.begin(uow, jo.method);
    // args
    dispatcher.handler(this.makeIdempotentHandler(replyTo, jo.args[jo.args.length-1]));
    dispatcher.dispatch();
}

JsonBeachhead.prototype.idem = function(uow, jo, replyTo) {
    const ih = this.broker.currentIdem(jo.idem);
    const um = new UnmarshalTraverser(uow, new CollectingState());
    const om = new ObjectMarshaller(uow, um);
    for (var i=0;i<jo.args.length;i++) {
        om.marshal(jo.args[i]);
    }
    uow.log("have args for", jo.method, "as", um.ret);
    um.ret[0].log("this works?", um.ret.length);
    ih[jo.method].apply(ih, um.ret);
}

JsonBeachhead.prototype.makeIdempotentHandler = function(replyTo, ihinfo) {
    var sender = replyTo;
    return {
        success: function(cx) {
            sender.send({action:"idem", method:"success", idem: ihinfo._ihid, args: []});
        },
        failure: function(cx, msg) {

        }
    }
}

const JsonMarshaller = function(broker, obj, sender) {
    this.broker = broker;
    this.obj = obj;
    this.sender = sender;
}

JsonMarshaller.prototype.string = function(s) {
    this.obj.args.push(s);
}

JsonMarshaller.prototype.handler = function(h) {
    this.obj.args.push({"_ihclz":"org.ziniki.ziwsh.intf.IdempotentHandler", "_ihid": this.broker.uniqueHandler(h)});
}

JsonMarshaller.prototype.dispatch = function() {
    this.sender.send(JSON.stringify(this.obj));
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
    this.logger.log("have zwc", zwc);
    this.logger.log("jb = ", JsonBeachhead);
    const bh = new JsonBeachhead(this.factory, uri, this, zwc);
    this.logger.log("have bh", bh);
    this.server = bh;
    zwc.attachBeachhead(bh);
    this.logger.log("attached", bh, "to", zwc);
    return zwc;
}

SimpleBroker.prototype.beachhead = function(bh) {
    this.server = bh;
}

SimpleBroker.prototype.register = function(clz, svc) {
    this.services[clz] = new UnmarshallerDispatcher(clz, svc);
}

SimpleBroker.prototype.require = function(clz) {
    var svc = this.services[clz];
    if (svc == null) {
        if (this.server != null)
            svc = this.server.unmarshalContract(clz);
        else
            return NoSuchContract.forContract(clz);
    }
    return new MarshallerProxy(this.logger, this.contracts[clz], svc).proxy;
}

SimpleBroker.prototype.unmarshalTo = function(clz) {
    var svc = this.services[clz];
    if (svc != null)
        return svc;
    else if (this.server != null)
        return this.server.unmarshalContract(clz);
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



const EvalContext = function(env) {
	this.env = env;
}

EvalContext.prototype.log = function(...args) {
	this.env.logger.log.apply(this.env.logger, args);
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
	this.cx.log('getting', fld, 'from', this, '=', ret);
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

IdempotentHandler.prototype.success = function(cx) {
};

IdempotentHandler.prototype.failure = function(cx, msg) {
};

const LoggingIdempotentHandler = function() {
};

LoggingIdempotentHandler.prototype = new IdempotentHandler();
LoggingIdempotentHandler.prototype.constructor = LoggingIdempotentHandler;

IdempotentHandler.prototype.success = function(cx) {
    cx.log("success");
};

IdempotentHandler.prototype.failure = function(cx, msg) {
    cx.log("failure: " + msg);
};



const MarshallerProxy = function(logger, ctr, svc) {
    this.logger = logger;
    this.svc = svc;
    this.proxy = proxy(logger, ctr, this);
}

MarshallerProxy.prototype.invoke = function(meth, args) {
    this.logger.log("MarshallerProxy." + meth + "(" + args.length + ")");
    for (var i=0;i<args.length;i++) {
        this.logger.log("arg", i, "=", args[i]);
    }
    
    const cx = args[0];
    const ux = this.svc.begin(cx, meth);
    new ArgListMarshaller(this.logger, false, true).marshal(ux, args);
    ux.dispatch();
    return null;
}

const ArgListMarshaller = function(logger, includeFirst, includeLast) {
    this.logger = logger;
    this.logger.log("created marshaller");
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
    this.logger.log("asked to send", o, typeof o);
    if (typeof o === "string")
        ux.string(o);
    else if (typeof o === "object") {
        if (o instanceof IdempotentHandler)
            ux.handler(o);
        else if (o.state instanceof FieldsContainer) {
            this.handleStruct(ux, o.state);
        } else {
            this.logger.log("o =", o);
            this.logger.log("o.state = ", o.state);
            throw Error("cannot handle object with constructor " + o.constructor.name);
        }
    } else
        throw Error("cannot handle " + typeof o);
}

ObjectMarshaller.prototype.handleStruct = function(ux, fc) {
    if (!fc.has("_type")) {
        throw new Error("No _type defined in " + fc);
    }
    const fm = ux.beginFields(fc.get("_type"));
    const ks = Object.keys(fc.dict);
    for (var k in ks) {
        fm.field(ks[k]);
        this.recursiveMarshal(fm, fc.dict[ks[k]]);
    }
    fm.complete();
}


const NoSuchContract = function(ctr) {
    this.ctr = ctr;
}

NoSuchContract.prototype.get = function(target, prop, receiver) {
    var ctr = this.ctr;
    var meth = String(prop);
    if (meth === "_proxyHandler")
        return this;
    else if (meth == "toString" || prop === Symbol.toStringTag) {
        return function() {
            return "noContract[" + ctr + "]";
        }
    } else if (meth === "inspect" || meth === "constructor") {
        return target.inspect;
    } else {
        return function(cx, ...rest) {
            cx.log("no such contract for", ctr, meth);
            const ih = rest[rest.length-1];
            ih.failure(cx, "There is no service for " + ctr + ":" + meth);
        }
    }
};

NoSuchContract.forContract = function(ctr) {
    return new Proxy({}, new NoSuchContract(ctr));
}


/** Create a proxy of a contract interface
 *  This may also apply to other things, but that's all I care about
 *  We need a:
 *    cx - a context (mainly used for logging)
 *    ctr - the *NAME* of the interface which must be defined in window.contracts
 *    handler - a class with a method defined called "invoke" which takes a method name and a list of arguments
 */

const proxy = function(cx, intf, handler) {
    const keys = Object.getOwnPropertyNames(intf).filter(k => k != 'constructor');
    const myhandler = {
        get: function(target, ps, receiver) {
            const prop = String(ps);
            if (prop === "_owner") {
                return handler;
            } else if (prop === "inspect" || ps === Symbol.toStringTag) {
                return target;
            }

            cx.log("invoke called on proxy for " + prop, keys);
            if (keys.includes(prop)) {
                const fn = function(...args) {
                    cx.log("invoking " + prop);
                    const ret = handler['invoke'].call(handler, prop, args);
                    cx.log("just invoked " + prop);
                    return ret;
                }
                return fn;
            } else {
                cx.log("there is no prop", prop);
                return function() {
                    return "-no-such-method-";
                };
            }
        }
    };
    var proxied = new Proxy({}, myhandler);
    return proxied;
}

const proxy1 = function(cx, underlying, methods, handler) {
    cx.log("mocking with methods", methods, typeof methods[0]);
    const myhandler = {
        get: function(target, ps, receiver) {
            const prop = String(ps);
            cx.log("Looking for", prop, "in", methods, methods.includes(prop));
            if (methods.includes(prop)) {
                const fn = function(...args) {
                    cx.log("invoking " + prop);
                    const ret = handler['invoke'].call(handler, prop, args);
                    cx.log("just invoked " + prop);
                    return ret;
                }
                return fn;
            } else if (target[prop]) {
                return target[prop];
            } else {
                cx.log("there is no prop", prop);
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
    return new DispatcherTraverser(this.svc, method, cx, new CollectingState());
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

CollectingTraverser.prototype.handler = function(h) {
    this.collect(h);
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
    this.cx.log("want to dispatch", this.svc, this.method);
    this.svc[this.method].apply(this.svc, this.ret);
    this.cx.log(this.cx);
    ih.success(this.cx);
}

const CollectingState = function() {

}


// TODO: I think I want to further remove the websocket abstraction from here
// so that I can test all the backlog/reconnect logic
// It needs to pass in a factory instead of URI, I think ...

const ZiwshWebClient = function(logger, factory, uri) {
    const zwc = this;
    this.logger = logger;
    logger.log("connecting to URI " + uri);
    this.conn = new WebSocket(uri);
    logger.log("have ws", this.conn);
    this.backlog = [];
    this.conn.addEventListener("error", (ev) => {
        logger.log("an error occurred");
    });
    this.conn.addEventListener("open", () => {
        logger.log("opened with backlog", this.backlog.length);
        while (this.backlog.length > 0) {
            const json = this.backlog.pop();
            logger.log("sending", json);
            this.conn.send(json);
        }
        logger.log("cleared backlog");
    });
    this.conn.addEventListener("message", (ev) => {
        logger.log("received a message", ev.data);
        logger.log("dispatching to", this.bh);
        this.bh.dispatch(factory.newContext(), ev.data, this);
    });
    logger.log("created ZWC");
}

ZiwshWebClient.prototype.attachBeachhead = function(bh) {
    this.bh = bh;
}

ZiwshWebClient.prototype.send = function(json) {
    this.logger.log("want to send " + json + " to " + this.bh.name + " state = " + this.conn.readyState);
    if (this.conn.readyState == 1)
        this.conn.send(json);
    else
        this.backlog.push(json);
}


/* istanbul ignore else */ 
if (typeof(module) !== 'undefined')
	module.exports = { EvalContext, FieldsContainer, JsonBeachhead };
else {
	window.EvalContext = EvalContext;
	window.FieldsContainer = FieldsContainer;
	window.JsonBeachhead = JsonBeachhead;
}
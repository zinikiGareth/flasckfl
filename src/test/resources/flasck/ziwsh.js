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



const SimpleBroker = function(logger, contracts) {
    this.logger = logger;
    this.contracts = contracts;
    this.services = {};
};

SimpleBroker.prototype.connectToServer = function(uri) {
    this.logger.log("connecting to URI " + uri);
    const webSocket = new WebSocket(uri);
    this.logger.log("have ws", webSocket);
    webSocket.addEventListener("open", () => {
        this.logger.log("opened");
    });
}

SimpleBroker.prototype.register = function(clz, svc) {
    this.services[clz] = new UnmarshallerDispatcher(clz, svc);
}

SimpleBroker.prototype.require = function(clz) {
    var svc = this.services[clz];
    if (svc == null) {
        return NoSuchContract.forContract(clz);
    }
    return new MarshallerProxy(this.logger, this.contracts[clz], svc).proxy;
}

SimpleBroker.prototype.unmarshalTo = function(clz) {
    return this.services[clz];
}




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
        else
            throw Error("cannot handle " + o.constructor.name);
    } else
        throw Error("cannot handle " + typeof o);
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



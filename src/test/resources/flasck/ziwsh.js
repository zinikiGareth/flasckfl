/** Create a proxy of a contract interface
 *  This may also apply to other things, but that's all I care about
 *  We need a:
 *    cx - a context (mainly used for logging)
 *    ctr - the *NAME* of the interface which must be defined in window.contracts
 *    handler - a class with a method defined called "invoke" which takes a method name and a list of arguments
 */

const proxy = function(cx, ctr, handler) {
    var intf = window.contracts[ctr].prototype;
    var proxied = new Object();
    Object.getOwnPropertyNames(intf).forEach(k => {
        if (k == "constructor")
            return;
        if (typeof(intf[k]) == "function") {
            cx.log("mocking", ctr, k);
            proxied[k] = function() {
                cx.log("invoking " + k);
                var args = [];
                for (var i=0;i<arguments.length;i++)
                    args[i] = arguments[i];
                var ret = handler["invoke"].call(handler, k, args);
                cx.log("just invoked " + k);
                return ret;
            }
        }
    });
    return proxied;
}

const proxy1 = function(cx, methods, handler) {
    var proxied = new Object();
    methods.forEach(k => {
        cx.log("mocking", k);
        proxied[k] = function() {
            cx.log("invoking " + k);
            var args = [];
            for (var i=0;i<arguments.length;i++)
                args[i] = arguments[i];
            var ret = handler["invoke"].call(handler, k, args);
            cx.log("just invoked " + k);
            return ret;
        }
    });
    return proxied;
}

window.proxy = proxy;
window.proxy1 = proxy1;
const SimpleBroker = function(logger) {
    this.logger = logger;
    this.services = {};
};

SimpleBroker.prototype.register = function(clz, svc) {
    this.services[clz] = new UnmarshallerDispatcher(clz, svc);
}

SimpleBroker.prototype.require = function(clz) {
    var ret = this.services[clz];
    this.logger.log("returning contract with", Object.getOwnPropertyNames(ret));
    return new MarshallerProxy(this.logger, clz, ret).proxy;
}

window.SimpleBroker = SimpleBroker;
const MarshallerProxy = function(logger, ctr, svc) {
    this.logger = logger;
    this.svc = svc;
    this.proxy = window.proxy(logger, ctr, this);
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
    ux.string(o);
}

window.MarshallerProxy = MarshallerProxy;
const UnmarshallerDispatcher = function(ctr, svc) {
    this.svc = svc;
}

UnmarshallerDispatcher.prototype.begin = function(cx, method) {
    return new DispatcherTraverser(this.svc, method, cx, new CollectingState());
}

const CollectingTraverser = function(cx, collector) {
    this.cx = cx;
    this.collector = collector;
}

CollectingTraverser.prototype.string = function(s) {
    this.collect(s);
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

window.UnmarshallerDispatcher = UnmarshallerDispatcher;

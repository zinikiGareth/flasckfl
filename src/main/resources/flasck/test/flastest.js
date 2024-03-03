// src/main/javascript/unittest/runner.js
import { CommonEnv } from "/js/flasjs.js";

// src/main/javascript/unittest/utcxt.js
import { FLContext } from "/js/flasjs.js";

// src/main/resources/ziwsh.js
var FieldsContainer = function(cx) {
  this.cx = cx;
  this.dict = {};
};
FieldsContainer.prototype.set = function(fld, val) {
  this.dict[fld] = val;
};
FieldsContainer.prototype.has = function(fld) {
  return !!this.dict[fld];
};
FieldsContainer.prototype.get = function(fld) {
  const ret = this.dict[fld];
  return ret;
};
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
};
FieldsContainer.prototype.toString = function() {
  return "Fields[" + Object.keys(this.dict).length + "]";
};
var EvalContext = function(env, broker) {
  this.env = env;
  this.broker = broker;
};
EvalContext.prototype.log = function(...args) {
  this.env.logger.log.apply(this.env.logger, args);
};
EvalContext.prototype.debugmsg = function(...args) {
  if (this.env.logger.debugmsg)
    this.env.logger.debugmsg.apply(this.env.logger, args);
  else
    this.log(args);
};
EvalContext.prototype.bindTo = function(to) {
  var ret = this.split();
  return ret;
};
EvalContext.prototype.split = function() {
  var ret = this.env.newContext();
  return ret;
};
EvalContext.prototype.registerContract = function(name, ctr) {
  if (this.broker && !this.broker.contracts[name])
    this.broker.contracts[name] = ctr;
};
EvalContext.prototype.registerStruct = function(name, str) {
  this.env.structs[name] = str;
};
EvalContext.prototype.structNamed = function(name) {
  return this.env.structs[name];
};
EvalContext.prototype.registerObject = function(name, str) {
  this.env.objects[name] = str;
};
EvalContext.prototype.objectNamed = function(name) {
  return this.env.objects[name];
};
EvalContext.prototype.fields = function() {
  return new FieldsContainer(this);
};
EvalContext.prototype.fromWire = function(om, fields) {
  var clz = this.env.objects[fields["_wireable"]];
  if (!clz) {
    throw Error("could not find a registration for " + fields["_wireable"]);
  }
  return clz.fromWire.call(clz, this, om, fields);
};
EvalContext.prototype._bindNamedHandler = function(nh) {
};
var IdempotentHandler = function() {
};
IdempotentHandler.prototype._clz = function() {
  return "org.ziniki.ziwsh.intf.IdempotentHandler";
};
IdempotentHandler.prototype.success = function(cx) {
};
IdempotentHandler.prototype.failure = function(cx, msg) {
};
IdempotentHandler.prototype._methods = function() {
  return ["success", "failure"];
};
var LoggingIdempotentHandler = function() {
};
LoggingIdempotentHandler.prototype = new IdempotentHandler();
LoggingIdempotentHandler.prototype.constructor = LoggingIdempotentHandler;
LoggingIdempotentHandler.prototype.success = function(cx) {
  cx.log("success");
};
LoggingIdempotentHandler.prototype.failure = function(cx, msg) {
  cx.log("failure: " + msg);
};
var NamedIdempotentHandler2 = function(handler, name) {
  this._handler = handler;
  this._name = name;
};
var Unmarshaller = function() {
};
var UnmarshallerDispatcher2 = function(ctr, svc) {
  this.svc = svc;
  Unmarshaller.call(this);
};
UnmarshallerDispatcher2.prototype = new Unmarshaller();
UnmarshallerDispatcher2.prototype.constructor = UnmarshallerDispatcher2;
UnmarshallerDispatcher2.prototype.begin = function(cx, method) {
  return new DispatcherTraverser(this.svc, method, cx, new CollectingState(cx));
};
UnmarshallerDispatcher2.prototype.toString = function() {
  return "ProxyFor[UnmarshallerDispatcher]";
};
var CollectingTraverser = function(cx, collector) {
  this.cx = cx;
  this.collector = collector;
};
CollectingTraverser.prototype.string = function(s) {
  this.collect(s);
};
CollectingTraverser.prototype.url = function(u) {
  this.collect(u);
};
CollectingTraverser.prototype.number = function(n) {
  this.collect(n);
};
CollectingTraverser.prototype.boolean = function(b) {
  this.collect(b);
};
CollectingTraverser.prototype.wireable = function(om, w) {
  this.collect(w);
};
CollectingTraverser.prototype.handleCycle = function(cr) {
  const already = this.collector.already(cr);
  if (already) {
    this.collect(this.collector.get(cr));
  }
  return already;
};
CollectingTraverser.prototype.circle = function(o, as) {
  this.collector.circle(o, as);
};
CollectingTraverser.prototype.unpack = function(collectingAs) {
  this.collector.circle(this.collector.nextName(), collectingAs);
};
CollectingTraverser.prototype.beginFields = function(clz) {
  const ft = new FieldsTraverser(this.cx, this.collector, clz);
  this.collect(ft.creation);
  return ft;
};
CollectingTraverser.prototype.beginList = function() {
  const lt = new ListTraverser(this.cx, this.collector);
  this.collect(lt.ret);
  return lt;
};
CollectingTraverser.prototype.handler = function(cx, h) {
  this.collect(h);
};
var FieldsTraverser = function(cx, collector, clz) {
  CollectingTraverser.call(this, cx, collector);
  var czz = cx.structNamed(clz);
  if (!czz)
    czz = cx.objectNamed(clz);
  if (!czz)
    throw new Error("Could not find a definition for " + clz);
  this.creation = new czz(cx);
  this.creation.state._wrapper = this.creation;
};
FieldsTraverser.prototype = new CollectingTraverser();
FieldsTraverser.prototype.constructor = FieldsTraverser;
FieldsTraverser.prototype.field = function(f) {
  this.currentField = f;
};
FieldsTraverser.prototype.collect = function(o) {
  this.creation.state.dict[this.currentField] = o;
  delete this.currentField;
};
FieldsTraverser.prototype.collectingAs = function() {
  return this.creation;
};
FieldsTraverser.prototype.complete = function() {
};
var ListTraverser = function(cx, collector) {
  CollectingTraverser.call(this, cx, collector);
  this.ret = [];
};
ListTraverser.prototype = new CollectingTraverser();
ListTraverser.prototype.constructor = ListTraverser;
ListTraverser.prototype.collect = function(o) {
  this.ret.push(o);
};
ListTraverser.prototype.complete = function() {
};
var UnmarshalTraverser = function(cx, collector) {
  ListTraverser.call(this, cx, collector);
  this.ret.push(cx);
};
UnmarshalTraverser.prototype = new ListTraverser();
UnmarshalTraverser.prototype.constructor = UnmarshalTraverser;
var DispatcherTraverser = function(svc, method, cx, collector) {
  UnmarshalTraverser.call(this, cx, collector);
  if (svc instanceof NamedIdempotentHandler2) {
    svc = svc._handler;
  }
  if (!svc[method])
    throw Error("no method '" + method + "': have " + JSON.stringify(Object.keys(svc)));
  this.svc = svc;
  this.method = method;
};
DispatcherTraverser.prototype = new UnmarshalTraverser();
DispatcherTraverser.prototype.constructor = DispatcherTraverser;
DispatcherTraverser.prototype.dispatch = function() {
  var cx = this.ret[0];
  var ih = this.ret[this.ret.length - 1];
  this.ret[0] = cx = cx.env.newContext().bindTo(this.svc);
  try {
    if (ih instanceof NamedIdempotentHandler2) {
      cx.log("have NIH in dispatch");
    }
    var rets = this.svc[this.method].apply(this.svc, this.ret);
  } catch (e) {
    cx.log("caught exception and reporting failure", e.toString());
    if (ih instanceof NamedIdempotentHandler2) {
      ih = ih._handler;
    }
    ih.failure(cx, e.message);
  }
  return rets;
};
var CollectingState = function(cx) {
  this.cx = cx;
  this.containing = [];
  this.named = {};
  this.next = 0;
};
CollectingState.prototype.nextName = function() {
  return "c" + this.next++;
};
CollectingState.prototype.circle = function(o, as) {
  this.containing.push({ obj: o, stored: as });
  if (typeof o === "string")
    this.named[o] = as;
};
CollectingState.prototype.already = function(cr) {
  if (this.named[cr])
    return true;
  for (var i = 0; i < this.containing.length; i++) {
    if (cr === this.containing[i].obj)
      return true;
  }
  return false;
};
CollectingState.prototype.get = function(cr) {
  if (this.named[cr])
    return this.named[cr];
  for (var i = 0; i < this.containing.length; i++) {
    if (cr === this.containing[i].obj)
      return this.containing[i].stored;
  }
  throw Error("no key" + cr);
};
var JsonBeachhead = function(factory, name, broker, sender) {
  this.factory = factory;
  this.name = name;
  this.broker = broker;
  this.sender = sender;
};
JsonBeachhead.prototype.unmarshalContract = function(contract) {
  var broker = this.broker;
  var sender = this.sender;
  return {
    begin: function(cx, method) {
      return new JsonArgsMarshaller(broker, { action: "invoke", contract, method, args: [] }, sender, new CollectingState(cx));
    }
  };
};
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
};
JsonBeachhead.prototype.invoke = function(uow, jo, replyTo) {
  const um = this.broker.unmarshalTo(jo.contract);
  const dispatcher = um.begin(uow, jo.method);
  for (var i = 0; i < jo.args.length - 1; i++) {
    const o = jo.args[i];
    this.handleArg(dispatcher, uow, o);
  }
  dispatcher.handler(uow, this.makeIdempotentHandler(replyTo, jo.args[jo.args.length - 1]));
  return dispatcher.dispatch();
};
JsonBeachhead.prototype.handleArg = function(ux, uow, o) {
  if (typeof o === "string")
    ux.string(o);
  else if (typeof o === "number")
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
    for (var k = 0; k < ks.length; k++) {
      fm.field(ks[k]);
      this.handleArg(fm, uow, o[ks[k]]);
    }
  } else
    throw Error("not handled: " + JSON.stringify(o));
};
JsonBeachhead.prototype.idem = function(uow, jo, replyTo) {
  const ih = this.broker.currentIdem(jo.idem);
  if (!ih) {
    uow.log("failed to find idem service for", jo.idem, new Error().stack);
    throw new Error("did not find idem " + jo.idem);
  }
  const um = new UnmarshallerDispatcher2(null, ih);
  const dispatcher = um.begin(uow, jo.method);
  var cnt = jo.args.length;
  var wantHandler = false;
  if (jo.method != "success" && jo.method != "failure") {
    wantHandler = true;
    cnt--;
  }
  for (var i = 0; i < cnt; i++) {
    this.handleArg(dispatcher, uow, jo.args[i]);
  }
  if (wantHandler) {
    const hi = this.makeIdempotentHandler(replyTo, jo.args[cnt - 1]);
    dispatcher.handler(uow, hi);
  }
  return dispatcher.dispatch();
};
JsonBeachhead.prototype.makeIdempotentHandler = function(replyTo, ihinfo) {
  var sender = replyTo;
  const ih = new IdempotentHandler();
  ih.success = function(cx) {
    sender.send({ action: "idem", method: "success", idem: ihinfo._ihid, args: [] });
  };
  ih.failure = function(cx, msg) {
  };
  return ih;
};
var JsonMarshaller = function(broker, sender, collector) {
  this.broker = broker;
  this.sender = sender;
  this.collector = collector;
};
JsonMarshaller.prototype.string = function(s) {
  this.collect(s);
};
JsonMarshaller.prototype.number = function(n) {
  this.collect(n);
};
JsonMarshaller.prototype.boolean = function(b) {
  this.collect(b);
};
JsonMarshaller.prototype.wireable = function(om, w) {
  var c = { _clz: "_wireable", "_wireable": w._clz };
  w._towire(c);
  this.collect(c);
};
JsonMarshaller.prototype.circle = function(o, as) {
  this.collector.circle(o, as);
};
JsonMarshaller.prototype.handleCycle = function(cr) {
  if (this.collector.already(cr)) {
    this.collect({ _cycle: this.collector.get(cr) });
    this.broker.logger.log("handled cycle", cr, new Error().stack);
    return true;
  }
  return false;
};
JsonMarshaller.prototype.beginFields = function(cls) {
  const me = { _clz: cls };
  this.collect(me);
  return new JsonFieldsMarshaller(this.broker, me, this.sender, this.collector);
};
JsonMarshaller.prototype.beginList = function(cls) {
  const me = [];
  this.collect(me);
  return new JsonListMarshaller(this.broker, me, this.sender, this.collector);
};
JsonMarshaller.prototype.handler = function(cx, h) {
  var clz, ihid;
  if (h instanceof NamedIdempotentHandler2) {
    cx.log("have NIH we want clz for", h);
    clz = h._handler._clz();
    ihid = h._ihid;
  } else {
    clz = h._clz();
    ihid = this.broker.uniqueHandler(h);
  }
  this.obj.args.push({ "_ihclz": clz, "_ihid": ihid });
};
JsonMarshaller.prototype.dispatch = function() {
  this.sender.send(JSON.stringify(this.obj));
};
var JsonArgsMarshaller = function(broker, obj, sender, collector) {
  JsonMarshaller.call(this, broker, sender, collector);
  this.obj = obj;
};
JsonArgsMarshaller.prototype = new JsonMarshaller();
JsonArgsMarshaller.prototype.constructor = JsonArgsMarshaller;
JsonArgsMarshaller.prototype.collect = function(o) {
  this.obj.args.push(o);
};
JsonArgsMarshaller.prototype.complete = function() {
};
var JsonFieldsMarshaller = function(broker, obj, sender, collector) {
  JsonMarshaller.call(this, broker, sender, collector);
  this.obj = obj;
  this.cas = collector.nextName();
};
JsonFieldsMarshaller.prototype = new JsonMarshaller();
JsonFieldsMarshaller.prototype.constructor = JsonFieldsMarshaller;
JsonFieldsMarshaller.prototype.collectingAs = function(o) {
  return this.cas;
};
JsonFieldsMarshaller.prototype.field = function(f) {
  this.currentField = f;
};
JsonFieldsMarshaller.prototype.collect = function(o) {
  this.obj[this.currentField] = o;
};
JsonFieldsMarshaller.prototype.complete = function() {
};
var JsonListMarshaller = function(broker, arr, sender, collector) {
  JsonMarshaller.call(this, broker, sender, collector);
  this.arr = arr;
};
JsonListMarshaller.prototype = new JsonMarshaller();
JsonListMarshaller.prototype.constructor = JsonListMarshaller;
JsonListMarshaller.prototype.collect = function(o) {
  this.arr.push(o);
};
JsonListMarshaller.prototype.complete = function() {
};
var OMWrapper = function(bh, cx, ux) {
  this.bh = bh;
  this.cx = cx;
  this.ux = ux;
  this.state = ux.state;
};
OMWrapper.prototype.marshal = function(trav, o) {
  this.bh.handleArg(trav, this.cx, o);
};
var proxy = function(cx, intf, handler) {
  var keys = intf._methods();
  if (!Array.isArray(keys)) {
    keys = Object.keys(keys);
  }
  if (intf instanceof IdempotentHandler) {
    keys.push("success");
    keys.push("failure");
  }
  const proxied = { _owner: handler };
  const methods = {};
  for (var i = 0; i < keys.length; i++) {
    const meth = keys[i];
    methods[meth] = proxied[meth] = proxyMeth(meth, handler);
  }
  proxied._methods = function() {
    return methods;
  };
  if (intf._clz) {
    var clz = intf._clz();
    proxied._clz = function() {
      return clz;
    };
  }
  if (intf._card) {
    proxied._card = intf._card;
  }
  return proxied;
};
var proxyMeth = function(meth, handler) {
  return function(...args) {
    const ret = handler["invoke"].call(handler, meth, args);
    return ret;
  };
};
var MarshallerProxy = function(logger, ctr, svc) {
  this.logger = logger;
  this.svc = svc;
  this.proxy = proxy(logger, ctr, this);
};
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
};
var ArgListMarshaller = function(logger, includeFirst, includeLast) {
  this.logger = logger;
  this.from = includeFirst ? 0 : 1;
  this.skipEnd = includeLast ? 0 : 1;
};
ArgListMarshaller.prototype.marshal = function(cx, m, args) {
  const om = new ObjectMarshaller(this.logger, m);
  for (var i = this.from; i < args.length - this.skipEnd; i++) {
    om.marshal(cx, args[i]);
  }
};
var ObjectMarshaller = function(logger, top) {
  this.logger = logger;
  this.top = top;
};
ObjectMarshaller.prototype.marshal = function(cx, o) {
  this.recursiveMarshal(cx, this.top, o);
};
ObjectMarshaller.prototype.recursiveMarshal = function(cx, ux, o) {
  if (o._throw && o._throw()) {
    cx.log("throwing because object has _throw");
    throw o;
  } else if (ux.handleCycle(o))
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
      var intf;
      if (o._clz && o._methods) {
        intf = o;
        cx.log("identified _clz as", intf._clz());
      } else {
        intf = Object.getPrototypeOf(o);
        var sintf = Object.getPrototypeOf(intf);
        cx.log("have IH with", intf, "and", sintf);
        if (typeof sintf._methods !== "undefined") {
          intf = sintf;
        }
      }
      var h = new NamedIdempotentHandler2(proxy(cx, intf, this.makeHandlerInvoker(cx, ihid)));
      h._ihid = ihid;
      ux.handler(cx, h);
    } else if (o instanceof NamedIdempotentHandler2) {
      cx.log("marshalling NIH with", o._ihid, "and", o._name, "and", o._handler.constructor);
      var ihid = cx.broker.uniqueHandler(o._handler);
      cx.log("concluded that ihid should be", ihid);
      o._ihid = ihid;
      cx._bindNamedHandler(o);
      var intf;
      if (o._handler._clz && o._handler._methods) {
        intf = o._handler;
        cx.log("identified NIH _clz as", intf._clz());
      } else {
        intf = Object.getPrototypeOf(o._handler);
        var sintf = Object.getPrototypeOf(intf);
        cx.log("have NIH with", intf, "and", sintf);
        if (sintf && typeof sintf._methods !== "undefined") {
          intf = sintf;
        }
      }
      var h = new NamedIdempotentHandler2(proxy(cx, intf, this.makeHandlerInvoker(cx, ihid)));
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
};
ObjectMarshaller.prototype.handleStruct = function(cx, ux, o) {
  const fc = o.state;
  if (!fc.has("_type")) {
    throw new Error("No _type defined in " + fc);
  }
  const fm = ux.beginFields(fc.get("_type"));
  ux.circle(o, fm.collectingAs());
  const ks = Object.keys(fc.dict);
  for (var k = 0; k < ks.length; k++) {
    fm.field(ks[k]);
    this.recursiveMarshal(cx, fm, fc.dict[ks[k]]);
  }
  fm.complete();
};
ObjectMarshaller.prototype.handleArray = function(cx, ux, l) {
  const ul = ux.beginList();
  for (var k = 0; k < l.length; k++) {
    this.recursiveMarshal(cx, ul, l[k]);
  }
  ul.complete();
};
ObjectMarshaller.prototype.makeHandlerInvoker = function(cx, ihid) {
  var broker = cx.broker;
  var env = cx.env;
  var handler = new Object();
  handler.invoke = function(name, args) {
    var uow = env.newContext();
    const ih = broker.currentIdem(ihid);
    if (!ih) {
      uow.log("failed to find idem handler for", ihid, new Error().stack);
      return;
    }
    const um = new UnmarshallerDispatcher(null, ih);
    const ux = um.begin(uow, name);
    new ArgListMarshaller(this.logger, false, true).marshal(cx, ux, args);
    return ux.dispatch();
  };
  return handler;
};
var NoSuchContract = function(ctr) {
  this.ctr = ctr;
};
var isntThere = function(ctr, meth) {
  return function(cx, ...rest) {
    cx.log("no such contract for", ctr.name(), meth);
    const ih = rest[rest.length - 1];
    const msg = "there is no service for " + ctr.name() + ":" + meth;
    throw Error(msg);
  };
};
NoSuchContract.forContract = function(ctr) {
  const nsc = new NoSuchContract(ctr);
  const ms = ctr._methods();
  const meths = {};
  for (var ni = 0; ni < ms.length; ni++) {
    var meth = ms[ni];
    meths[meth] = nsc[meth] = isntThere(ctr, meth);
  }
  nsc._methods = function() {
    return meths;
  };
  return nsc;
};
var ZiwshWebClient = function(logger, factory, uri) {
  this.logger = logger;
  this.factory = factory;
  if (uri) {
    this.connectTo(uri);
  }
  logger.log("created ZWC with uri", uri);
};
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
    const cx = zwc.factory.newContext();
    var actions = this.bh.dispatch(cx, ev.data, this);
    if (zwc.factory.queueMessages) {
      zwc.factory.queueMessages(cx, actions);
    }
  });
};
ZiwshWebClient.prototype.attachBeachhead = function(bh) {
  this.bh = bh;
};
ZiwshWebClient.prototype.send = function(json) {
  this.logger.log("want to send " + json + " to " + this.bh.name + (this.conn ? this.conn.readyState == 1 ? "" : "; not ready; state = " + this.conn.readyState : " not connected"));
  if (this.conn && this.conn.readyState == 1)
    this.conn.send(json);
  else
    this.backlog.push(json);
};
var zwc_default = ZiwshWebClient;
var brokerId = 1;
var SimpleBroker = function(logger, factory, contracts) {
  this.logger = logger;
  this.server = null;
  this.factory = factory;
  this.contracts = contracts;
  this.services = {};
  this.nextHandle = 1;
  this.handlers = {};
  this.serviceHandlers = /* @__PURE__ */ new Map();
  this.name = "jsbroker_" + brokerId++;
  logger.log(
    "created ",
    this.name
    /*, new Error().stack */
  );
};
SimpleBroker.prototype.connectToServer = function(uri) {
  const zwc = new zwc_default(this.logger, this.factory, uri);
  const bh = new JsonBeachhead(this.factory, uri, this, zwc);
  this.server = bh;
  zwc.attachBeachhead(bh);
  this.logger.log("attached", bh, "to", zwc);
  return zwc;
};
SimpleBroker.prototype.updateConnection = function(uri) {
  this.server.sender.connectTo(uri);
};
SimpleBroker.prototype.awaitingServerConnection = function() {
  return this.server && !this.server.sender.conn;
};
SimpleBroker.prototype.beachhead = function(bh) {
  this.server = bh;
};
SimpleBroker.prototype.register = function(clz, svc) {
  this.services[clz] = new UnmarshallerDispatcher2(clz, svc);
};
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
      svc = new UnmarshallerDispatcher2(clz, NoSuchContract.forContract(ctr));
  }
  return new MarshallerProxy(this.logger, ctr, svc).proxy;
};
SimpleBroker.prototype.unmarshalTo = function(clz) {
  var svc = this.services[clz];
  if (svc != null)
    return svc;
  else if (this.server != null) {
    return this.server.unmarshalContract(clz);
  } else
    return NoSuchContract.forContract(clz);
};
SimpleBroker.prototype.uniqueHandler = function(h) {
  const name = "handler_" + this.nextHandle++;
  this.handlers[name] = h;
  this.logger.log("registered handler name", name, "in", this.name, "have", JSON.stringify(Object.keys(this.handlers)));
  return name;
};
SimpleBroker.prototype.currentIdem = function(h) {
  const ret = this.handlers[h];
  if (!ret) {
    this.logger.log("there is no handler for", h, "in", this.name, "have", JSON.stringify(Object.keys(this.handlers)));
  }
  return ret;
};
SimpleBroker.prototype.serviceFor = function(h, sf) {
  if (!h._ihid) {
    throw new Error("must have an _ihid");
  }
  this.serviceHandlers.set(h._ihid, sf);
};
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
};
var broker_default = SimpleBroker;

// src/main/javascript/unittest/mocks.js
var BoundVar = function(name) {
  this.name = name;
};
BoundVar.prototype.bindActual = function(obj) {
  if (this.actual) {
    throw Error("cannot rebind bound var");
  }
  this.actual = obj;
};
BoundVar.prototype.introduced = function() {
  if (!this.actual)
    throw Error("bound var has not yet been bound");
  return this.actual;
};
var Expectation = function(args, handler) {
  this.args = args;
  this.handler = handler;
  this.allowed = 1;
  this.invoked = 0;
};
Expectation.prototype.allow = function(n) {
  this.allowed = n;
};
var proxyMe = function(self, meth) {
  return function(cx, ...rest) {
    self.serviceMethod(cx, meth, rest);
  };
};
var MockContract = function(ctr) {
  this.ctr = ctr;
  this.expected = {};
  this.methodNames = {};
  var ms = ctr._methods();
  for (var i in ms) {
    this.methodNames[ms[i]] = this[ms[i]] = proxyMe(this, ms[i]);
  }
  this._methods = function() {
    return this.methodNames;
  };
};
MockContract.prototype._areYouA = function(cx, ty) {
  return this.ctr.name() == ty;
};
MockContract.prototype.expect = function(meth, args, handler) {
  if (!this.expected[meth])
    this.expected[meth] = [];
  if (!this.ctr[meth] || !this.ctr[meth].nfargs) {
    throw new Error("EXP\n  " + this.ctr.name() + " does not have a method " + meth);
  }
  const expArgs = this.ctr[meth].nfargs();
  if (args.length != expArgs) {
    throw new Error("EXP\n  " + this.ctr.name() + "." + meth + " expects " + expArgs + " parameters, not " + args.length);
  }
  const exp = new Expectation(args, handler);
  this.expected[meth].push(exp);
  return exp;
};
MockContract.prototype.serviceMethod = function(_cxt, meth, args) {
  if (meth === "success" || meth === "failure")
    return;
  const ih = args[args.length - 1];
  args = args.slice(0, args.length - 1);
  if (!this.expected[meth]) {
    _cxt.env.error(new Error("EXP\n  There are no expectations on " + this.ctr.name() + " for " + meth));
    return;
  }
  const exp = this.expected[meth];
  var pending = null;
  for (var i = 0; i < exp.length; i++) {
    if (_cxt.compare(exp[i].args, args)) {
      var matched = exp[i];
      if (matched.invoked == matched.allowed) {
        pending = new Error("EXP\n  " + this.ctr.name() + "." + meth + " " + args + " already invoked (allowed=" + matched.allowed + "; actual=" + matched.invoked + ")");
        continue;
      }
      matched.invoked++;
      _cxt.log("Have invocation of", meth, "with", args);
      if (matched.handler instanceof BoundVar) {
        var tih = ih;
        matched.handler.bindActual(tih);
        if (ih._ihid) {
          _cxt.broker.serviceFor(ih, new SubscriptionFor(matched.handler.name, ih._ihid));
        }
      }
      return;
    }
  }
  if (pending) {
    _cxt.env.error(pending);
    return;
  } else {
    _cxt.env.error(new Error("EXP\n  Unexpected invocation: " + this.ctr.name() + "." + meth + " " + args));
    return;
  }
};
MockContract.prototype.assertSatisfied = function(_cxt) {
  var msg = "";
  for (var meth in this.expected) {
    if (!this.expected.hasOwnProperty(meth))
      continue;
    var exp = this.expected[meth];
    for (var i = 0; i < exp.length; i++) {
      if (exp[i].invoked != exp[i].allowed)
        msg += "  " + this.ctr.name() + "." + meth + " <" + i + ">\n";
    }
  }
  if (msg)
    throw new Error("UNUSED\n" + msg);
};
var SubscriptionFor = function(varName, handlerName) {
  this.varName = varName;
  this.handlerName = handlerName;
};
SubscriptionFor.prototype.cancel = function(cx) {
  cx.env.cancelBound(this.varName, this.handlerName);
};
var MockFLObject = function(obj) {
  this.obj = obj;
};
MockFLObject.prototype._currentDiv = function() {
  if (this.div)
    return this.div;
  else
    return null;
};
MockFLObject.prototype._currentRenderTree = function() {
  if (this.rt)
    return this.rt.result.single;
  else
    return null;
};
var MockAgent2 = function(agent) {
  this.agent = agent;
};
MockAgent2.prototype.sendTo = function(_cxt, contract, msg, args) {
  const ctr = this.agent._contracts.contractFor(_cxt, contract);
  const inv = Array.from(args);
  inv.splice(0, 0, _cxt);
  return ctr[msg].apply(ctr, inv);
};
var MockCard = function(cx, card) {
  this.card = card;
  const newdiv = document.createElement("div");
  newdiv.setAttribute("id", cx.nextDocumentId());
  document.body.appendChild(newdiv);
  this.card._renderInto(cx, newdiv);
};
MockCard.prototype.sendTo = function(_cxt, contract, msg, args) {
  const ctr = this.card._contracts.contractFor(_cxt, contract);
  const inv = Array.from(args);
  inv.splice(0, 0, _cxt);
  return ctr[msg].apply(ctr, inv);
};
MockCard.prototype._currentDiv = function() {
  return this.card._currentDiv();
};
MockCard.prototype._currentRenderTree = function() {
  return this.card._renderTree;
};
MockCard.prototype._underlying = function(_cxt) {
  return this.card;
};
var ExplodingIdempotentHandler = function(cx) {
  this.cx = cx;
  this.successes = { expected: 0, actual: 0 };
  this.failures = [];
};
ExplodingIdempotentHandler.prototype = new IdempotentHandler();
ExplodingIdempotentHandler.prototype.constructor = ExplodingIdempotentHandler;
ExplodingIdempotentHandler.prototype.success = function(cx) {
  cx.log("success");
};
ExplodingIdempotentHandler.prototype.failure = function(cx, msg) {
  cx.log("failure: " + msg);
  for (var i = 0; i < this.failures.length; i++) {
    const f = this.failures[i];
    if (f.msg === msg) {
      f.actual++;
      return;
    }
  }
  this.failures.push({ msg, expected: 0, actual: 1 });
};
ExplodingIdempotentHandler.prototype.expectFailure = function(msg) {
  this.cx.log("expect failure: " + msg);
  this.failures.push({ msg, expected: 1, actual: 0 });
};
ExplodingIdempotentHandler.prototype.assertSatisfied = function() {
  var msg = "";
  for (var i = 0; i < this.failures.length; i++) {
    const f = this.failures[i];
    if (f.expected === 0) {
      msg += "  failure: unexpected IH failure: " + f.msg;
    } else if (f.expected != f.actual) {
      msg += "  failure: " + f.msg + " (expected: " + f.expected + ", actual: " + f.actual + ")\n";
    }
  }
  if (msg)
    throw new Error("HANDLERS\n" + msg);
};
var MockHandler2 = function(ctr) {
  this.successes = { expected: 0, actual: 0 };
  this.failures = [];
  this.ctr = ctr;
  this.expected = {};
  this.methodNames = {};
  var ms = ctr._methods();
  for (var i in ms) {
    this.methodNames[ms[i]] = this[ms[i]] = proxyMe(this, ms[i]);
  }
  this._methods = function() {
    return this.methodNames;
  };
};
MockHandler2.prototype = new ExplodingIdempotentHandler();
MockHandler2.prototype.constructor = MockHandler2;
MockHandler2.prototype._areYouA = MockContract.prototype._areYouA;
MockHandler2.prototype.expect = MockContract.prototype.expect;
MockHandler2.prototype.serviceMethod = MockContract.prototype.serviceMethod;
MockHandler2.prototype.assertSatisfied = MockContract.prototype.assertSatisfied;
var MockAjax2 = function(_cxt, baseUri) {
  this.baseUri = baseUri;
  this.expect = { subscribe: [] };
};
MockAjax2.prototype.expectSubscribe = function(_cxt, path) {
  var mas = new MockAjaxSubscriber(_cxt, path);
  this.expect.subscribe.push(mas);
  return mas;
};
MockAjax2.prototype.pump = function(_cxt) {
  for (var i = 0; i < this.expect.subscribe.length; i++) {
    this.expect.subscribe[i].dispatch(_cxt, this.baseUri, _cxt.env.activeSubscribers);
  }
};
var MockAjaxSubscriber = function(_cxt, path) {
  this.path = path;
  this.responses = [];
  this.nextResponse = 0;
};
MockAjaxSubscriber.prototype.response = function(_cxt, val) {
  this.responses.push(val);
};
MockAjaxSubscriber.prototype.dispatch = function(_cxt, baseUri, subscribers) {
  if (this.nextResponse >= this.responses.length)
    return;
  for (var i = 0; i < subscribers.length; i++) {
    if (this.matchAndSend(_cxt, baseUri, subscribers[i]))
      return;
  }
};
MockAjaxSubscriber.prototype.matchAndSend = function(_cxt, baseUri, sub) {
  if (sub.uri.toString() == new URL(this.path, baseUri).toString()) {
    var resp = this.responses[this.nextResponse++];
    resp = _cxt.full(resp);
    if (resp instanceof FLError) {
      _cxt.log(resp);
      return true;
    }
    var msg;
    if (resp instanceof AjaxMessage) {
      msg = resp;
    } else {
      msg = new AjaxMessage(_cxt);
      msg.state.set("headers", []);
      if (typeof resp === "string")
        msg.state.set("body", resp);
      else
        msg.state.set("body", JSON.stringify(resp));
    }
    _cxt.env.queueMessages(_cxt, Send.eval(_cxt, sub.handler, "message", [msg], null));
    _cxt.env.dispatchMessages(_cxt);
    return true;
  } else
    return false;
};
var MockAjaxService = function() {
};
MockAjaxService.prototype.subscribe = function(_cxt, uri, options, handler) {
  if (uri instanceof FLURI)
    uri = uri.uri;
  _cxt.env.activeSubscribers.push({ uri, options, handler });
};
var MockAppl = function(_cxt, clz) {
  const newdiv = document.createElement("div");
  newdiv.setAttribute("id", _cxt.nextDocumentId());
  document.body.appendChild(newdiv);
  this.appl = new clz._Application(_cxt, newdiv);
  this.appl._updateDisplay(_cxt, this.appl._currentRenderTree());
};
MockAppl.prototype.route = function(_cxt, r, andThen) {
  this.appl.gotoRoute(_cxt, r, () => {
    this.appl._updateDisplay(_cxt, this.appl._currentRenderTree());
    andThen();
  });
};
MockAppl.prototype.userLoggedIn = function(_cxt, u) {
  this.appl.securityModule.userLoggedIn(_cxt, this.appl, u);
};
MockAppl.prototype.bindCards = function(_cxt, iv) {
  if (!iv)
    return;
  var binding = {};
  binding["main"] = this.appl.cards["main"];
  iv.bindActual({ routes: binding });
};
MockAppl.prototype._currentRenderTree = function() {
  return this.appl._currentRenderTree();
};

// src/main/javascript/unittest/utcxt.js
var UTContext = function(env, broker) {
  FLContext.call(this, env, broker);
};
UTContext.prototype = FLContext.prototype;
UTContext.prototype.constructor = UTContext;
UTContext.prototype.storeMock = function(name, value) {
  value = this.full(value);
  if (value instanceof ResponseWithMessages) {
    this.env.queueMessages(this, ResponseWithMessages.messages(this, value));
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
};
UTContext.prototype.mockContract = function(contract) {
  const ret = new MockContract(contract);
  this.broker.register(contract.name(), ret);
  return ret;
};
UTContext.prototype.mockAgent = function(agent) {
  return this.env.mockAgent(this, agent);
};
UTContext.prototype.mockCard = function(name, card) {
  return this.env.mockCard(this, name, card);
};
UTContext.prototype.explodingHandler = function() {
  const ret = new ExplodingIdempotentHandler(this);
  return ret;
};
UTContext.prototype.mockHandler = function(contract) {
  const ret = new MockHandler(contract);
  return ret;
};

// src/main/javascript/unittest/runner.js
import { FLError as FLError2 } from "/js/flasjs.js";
import { Debug, Send as Send2, Assign, ResponseWithMessages as ResponseWithMessages2, UpdateDisplay } from "/js/flasjs.js";
var UTRunner = function(bridge) {
  if (!bridge)
    bridge = console;
  CommonEnv.call(this, bridge, new broker_default(bridge, this, {}));
  this.errors = [];
  this.mocks = {};
  this.ajaxen = [];
  this.appls = [];
  this.activeSubscribers = [];
  if (typeof window !== "undefined")
    window.utrunner = this;
  this.moduleInstances = {};
  this.toCancel = /* @__PURE__ */ new Map();
  for (var mn in UTRunner.modules) {
    if (UTRunner.modules.hasOwnProperty(mn)) {
      var jm;
      if (bridge.module) {
        jm = bridge.module(this, mn);
        if (jm == "must-wait")
          continue;
      }
      this.moduleInstances[mn] = new UTRunner.modules[mn](this, jm);
    }
  }
};
UTRunner.prototype = new CommonEnv();
UTRunner.prototype.constructor = UTRunner;
UTRunner.prototype.newContext = function() {
  return new UTContext(this, this.broker);
};
UTRunner.modules = {};
UTRunner.prototype.bindModule = function(name, jm) {
  this.moduleInstances[name] = new UTRunner.modules[name](this, jm);
};
UTRunner.prototype.makeReady = function() {
  CommonEnv.prototype.makeReady.call(this);
  this.broker.register("Ajax", new MockAjaxService());
};
UTRunner.prototype.error = function(err) {
  this.errors.push(err);
};
UTRunner.prototype.handleMessages = function(_cxt, msg) {
  return CommonEnv.prototype.handleMessages.call(this, _cxt, msg);
};
UTRunner.prototype.assertSameValue = function(_cxt, e, a) {
  e = _cxt.full(e);
  if (e instanceof ResponseWithMessages2)
    e = e.obj;
  a = _cxt.full(a);
  if (!_cxt.compare(e, a)) {
    if (a instanceof FLError2)
      a = a.message;
    throw new Error("NSV\n  expected: " + e + "\n  actual:   " + a);
  }
};
UTRunner.prototype.assertIdentical = function(_cxt, e, a) {
  e = _cxt.full(e);
  a = _cxt.full(a);
  if (a !== e) {
    if (a instanceof FLError2)
      a = a.message;
    throw new Error("NSV\n  expected: " + e + "\n  actual:   " + a);
  }
};
UTRunner.prototype.shove = function(_cxt, dest, slot, val) {
  dest = _cxt.full(dest);
  val = _cxt.full(val);
  if (dest instanceof MockCard) {
    dest = dest.card;
  }
  dest.state.set(slot, val);
  if (dest._updateDisplay)
    dest._updateDisplay(_cxt, dest._renderTree);
  else {
    this.updateAllCards(_cxt);
  }
};
UTRunner.prototype.close = function(_cxt, dest) {
  dest = _cxt.full(dest);
  if (dest instanceof MockCard) {
    dest = dest.card;
  }
  dest._close(_cxt);
  this.updateAllCards(_cxt);
};
UTRunner.prototype.invoke = function(_cxt, inv) {
  inv = _cxt.full(inv);
  if (inv instanceof Array && inv.length == 1) {
    inv = inv[0];
  }
  var tcx;
  if (inv instanceof Send2)
    tcx = _cxt.bindTo(inv.obj);
  else
    tcx = _cxt.split();
  this.queueMessages(tcx, inv);
  this.dispatchMessages(tcx);
};
UTRunner.prototype.send = function(_cxt, target, contract, msg, inargs) {
  _cxt.log("doing send from runner to " + contract + ":" + msg);
  var reply;
  var args = [];
  for (var i = 0; i < inargs.length; i++) {
    if (inargs[i] instanceof MockCard) {
      args.push(inargs[i].card);
    } else {
      args.push(inargs[i]);
    }
  }
  var tcx = _cxt.bindTo(target);
  if (target.sendTo) {
    reply = target.sendTo(tcx, contract, msg, args);
  } else {
    var withArgs = args.slice();
    withArgs.unshift(tcx);
    reply = target[msg].apply(target, withArgs);
  }
  reply = tcx.full(reply);
  this.queueMessages(tcx, reply);
  this.dispatchMessages(tcx);
  this.updateCard(tcx, target);
};
UTRunner.prototype.render = function(_cxt, target, fn, template) {
  var sendTo = this.findMockFor(target);
  if (!sendTo)
    throw Error("there is no mock " + target);
  sendTo.rt = {};
  if (sendTo.div) {
    sendTo.div.innerHTML = "";
  } else {
    const newdiv = document.createElement("div");
    newdiv.setAttribute("id", _cxt.nextDocumentId());
    document.body.appendChild(newdiv);
    sendTo.div = newdiv;
    sendTo.rt._id = newdiv.id;
  }
  const mr = document.createElement("div");
  mr.setAttribute("data-flas-mock", "result");
  sendTo.div.appendChild(mr);
  sendTo.redraw = function(cx) {
    sendTo.obj._updateTemplate(cx, sendTo.rt, "mock", "result", fn, template, sendTo.obj, []);
  };
  sendTo.redraw(_cxt);
};
UTRunner.prototype.findMockFor = function(obj) {
  if (obj instanceof MockFLObject || obj instanceof MockCard || obj instanceof MockAppl)
    return obj;
  var ks = Object.keys(this.mocks);
  for (var i = 0; i < ks.length; i++) {
    if (this.mocks[ks[i]].obj == obj)
      return this.mocks[ks[i]];
  }
  throw new Error("no mock for " + obj);
};
UTRunner.prototype.event = function(_cxt, target, zone, event) {
  var sendTo = this.findMockFor(target);
  if (!sendTo)
    throw Error("there is no mock " + target);
  var div = null;
  var receiver;
  if (sendTo instanceof MockCard)
    receiver = sendTo.card;
  else if (sendTo instanceof MockFLObject)
    receiver = sendTo;
  else
    throw Error("cannot send event to " + target);
  if (!zone || zone.length == 0) {
    div = receiver._currentDiv();
  } else
    div = this.findDiv(_cxt, receiver._currentRenderTree(), zone, 0);
  if (div) {
    div.dispatchEvent(event._makeJSEvent(_cxt, div));
    this.dispatchMessages(_cxt);
  }
};
UTRunner.prototype.input = function(_cxt, target, zone, text) {
  var sendTo = this.findMockFor(target);
  if (!sendTo)
    throw Error("there is no mock " + target);
  var receiver;
  if (sendTo instanceof MockCard)
    receiver = sendTo.card;
  else if (sendTo instanceof MockFLObject)
    receiver = sendTo;
  else
    throw Error("cannot send event to " + target);
  var div = this.findDiv(_cxt, receiver._currentRenderTree(), zone, 0);
  if (div) {
    text = _cxt.full(text);
    if (text instanceof Error) {
      _cxt.log(text);
      return;
    }
    if (!div.tagName == "INPUT" || !div.hasAttribute("type") || div.getAttribute("type") != "text" && div.getAttribute("type") != "password") {
      _cxt.log("can only set input text on input elements of type text or password");
      return;
    }
    div.setAttribute("value", text);
  }
};
UTRunner.prototype.findDiv = function(_cxt, rt, zone, pos) {
  if (!rt) {
    throw Error("MATCH\nThe card has not been rendered");
  }
  if (pos >= zone.length) {
    return document.getElementById(rt._id);
  }
  const first = zone[pos];
  if (first[0] == "item") {
    if (!rt.children || first[1] >= rt.children.length) {
      throw Error("MATCH\nMatcher failed on '" + this._nameOf(zone, zone.length) + "': There is no child " + first[1] + " of " + this._nameOf(zone, pos));
    }
    return this.findDiv(_cxt, rt.children[first[1]], zone, pos + 1);
  } else {
    const inner = this._findSubThroughSingle(rt, first[1]);
    if (!inner) {
      throw Error("MATCH\nThere is no element " + first[1] + " in " + this._nameOf(zone, pos));
    }
    return this.findDiv(_cxt, inner, zone, pos + 1);
  }
};
UTRunner.prototype._findSubThroughSingle = function(rt, name) {
  while (true) {
    var ret = rt[name];
    if (ret)
      return ret;
    rt = rt["single"];
    if (rt == null)
      return null;
  }
};
UTRunner.prototype._nameOf = function(zone, pos) {
  if (pos == 0)
    return "card";
  var ret = "";
  for (var i = 0; i < pos; i++) {
    if (ret)
      ret += ".";
    ret += zone[i][1];
  }
  return ret;
};
UTRunner.prototype.getZoneDiv = function(_cxt, target, zone) {
  if (!target || !target._currentRenderTree) {
    throw Error("MATCH\nThe card has no rendered content");
  }
  return this.findDiv(_cxt, target._currentRenderTree(), zone, 0);
};
UTRunner.prototype.matchText = function(_cxt, target, zone, contains, fails, expected) {
  var matchOn = this.findMockFor(target);
  if (!matchOn)
    throw Error("there is no mock " + target);
  try {
    var div = this.getZoneDiv(_cxt, matchOn, zone);
  } catch (e) {
    if (fails)
      return;
    else
      throw e;
  }
  var actual = div.innerText.trim();
  actual = actual.replace(/\n/g, " ");
  actual = actual.replace(/ +/, " ");
  actual = actual.trim();
  if (contains) {
    if (!actual.includes(expected))
      throw new Error("MATCH\n  expected to contain: " + expected + "\n  actual:   " + actual);
  } else {
    if (actual != expected)
      throw new Error("MATCH\n  expected: " + expected + "\n  actual:   " + actual);
  }
};
UTRunner.prototype.matchTitle = function(_cxt, target, zone, contains, expected) {
  var matchOn = this.findMockFor(target);
  if (!matchOn)
    throw Error("there is no mock " + target);
  if (!(matchOn instanceof MockAppl))
    throw Error("can only test title on Appl");
  var titles = document.head.getElementsByTagName("title");
  var actual = "";
  for (var i = 0; i < titles.length; i++) {
    actual += titles[i].innerText.trim() + " ";
  }
  actual = actual.trim();
  actual = actual.replace(/\n/g, " ");
  actual = actual.replace(/ +/, " ");
  if (contains) {
    if (!actual.includes(expected))
      throw new Error("MATCH\n  expected to contain: " + expected + "\n  actual:   " + actual);
  } else {
    if (actual != expected)
      throw new Error("MATCH\n  expected: " + expected + "\n  actual:   " + actual);
  }
};
UTRunner.prototype.matchImageUri = function(_cxt, target, zone, expected) {
  var matchOn = this.findMockFor(target);
  if (!matchOn)
    throw Error("there is no mock " + target);
  var div = this.getZoneDiv(_cxt, matchOn, zone);
  if (div.tagName != "IMG")
    throw new Error("MATCH\n  expected: IMG\n  actual:   " + div.tagName);
  var abs = new URL(expected, window.location).toString();
  var actual = div.src;
  if (actual != abs)
    throw new Error("MATCH\n  expected: " + abs + "\n  actual:   " + actual);
};
UTRunner.prototype.matchHref = function(_cxt, target, zone, expected) {
  var matchOn = this.findMockFor(target);
  if (!matchOn)
    throw Error("there is no mock " + target);
  var div = this.getZoneDiv(_cxt, matchOn, zone);
  if (div.tagName != "A")
    throw new Error("MATCH\n  expected: A\n  actual:   " + div.tagName);
  var abs = expected;
  var actual = div.dataset.route.toString();
  if (actual != abs)
    throw new Error("MATCH\n  expected: " + abs + "\n  actual:   " + actual);
};
UTRunner.prototype.matchStyle = function(_cxt, target, zone, contains, expected) {
  var matchOn = this.findMockFor(target);
  if (!matchOn)
    throw Error("there is no mock " + target);
  var div = this.getZoneDiv(_cxt, matchOn, zone);
  var clzlist = div.getAttribute("class");
  if (!clzlist)
    clzlist = "";
  clzlist = clzlist.trim().split(" ").sort();
  var explist = expected.trim().split(" ").sort();
  var failed = false;
  for (var i = 0; i < explist.length; i++) {
    var exp = explist[i];
    failed |= !clzlist.includes(exp);
  }
  if (!contains)
    failed |= clzlist.length != explist.length;
  if (failed) {
    if (contains)
      throw new Error("MATCH\n  expected to contain: " + explist.join(" ") + "\n  actual: " + clzlist.join(" "));
    else
      throw new Error("MATCH\n  expected: " + explist.join(" ") + "\n  actual:   " + clzlist.join(" "));
  }
};
UTRunner.prototype.matchScroll = function(_cxt, target, zone, contains, expected) {
  var matchOn = this.findMockFor(target);
  if (!matchOn)
    throw Error("there is no mock " + target);
  var div = this.getZoneDiv(_cxt, matchOn, zone);
  var actual = div.scrollTop;
  if (actual != expected)
    throw new Error("MATCH\n  expected: " + expected + "\n  actual:   " + actual);
};
UTRunner.prototype.route = function(_cxt, app, route, storeCards) {
  app.route(_cxt, route, () => {
    app.bindCards(_cxt, storeCards);
  });
};
UTRunner.prototype.userlogin = function(_cxt, app, user) {
  app.userLoggedIn(_cxt, user);
};
UTRunner.prototype.updateCard = function(_cxt, card) {
  if (!(card instanceof MockCard))
    return;
  if (card.card._updateDisplay)
    card.card._updateDisplay(_cxt, card.card._renderTree);
};
UTRunner.prototype.checkAtEnd = function() {
  if (this.errors.length > 0)
    throw this.errors[0];
};
UTRunner.prototype.newdiv = function(cnt) {
  if (cnt != null) {
    if (cnt != this.nextDivId - this.divSince) {
      throw Error("NEWDIV\n  expected: " + cnt + "\n  actual:   " + (this.nextDivId - this.divSince));
    }
  }
  this.divSince = this.nextDivId;
};
UTRunner.prototype.expectCancel = function(handler) {
  var hn;
  if (handler instanceof NamedIdempotentHandler) {
    hn = handler._ihid;
  } else {
    throw new Error("not handled");
  }
  this.toCancel.set(hn, handler);
};
UTRunner.prototype.cancelBound = function(varName, handlerName) {
  if (!this.toCancel.has(handlerName)) {
    throw new Error("UECAN\n  cancelled " + varName + " but it was not expected");
  }
  this.toCancel.delete(handlerName);
};
UTRunner.prototype.assertSatisfied = function() {
  if (this.toCancel.size != 0) {
    throw new Error("EXPCAN\n  subscription was not cancelled");
  }
};
UTRunner.prototype.mockAgent = function(_cxt, agent) {
  return new MockAgent(agent);
};
UTRunner.prototype.mockCard = function(_cxt, name, card) {
  var ret = new MockCard(_cxt, card);
  this.mocks[name] = ret;
  this.cards.push(ret);
  return ret;
};
UTRunner.prototype.newAjax = function(cxt, baseUri) {
  var ma = new MockAjax(cxt, baseUri);
  this.ajaxen.push(ma);
  return ma;
};
UTRunner.prototype.newMockAppl = function(cxt, clz) {
  var ma = new MockAppl(cxt, clz);
  this.appls.push(ma);
  return ma;
};
UTRunner.prototype._updateDisplay = function(_cxt, rt) {
  this.updateAllCards(_cxt);
};
UTRunner.prototype.updateAllCards = function(_cxt) {
  for (var i = 0; i < this.cards.length; i++) {
    var mo = this.cards[i];
    if (mo instanceof MockFLObject) {
      if (mo.redraw)
        mo.redraw(_cxt);
    } else {
      var c = mo.card;
      if (c._updateDisplay)
        c._updateDisplay(_cxt, c._renderTree);
    }
  }
};
UTRunner.prototype.module = function(mod) {
  var m = this.moduleInstances[mod];
  if (!m)
    throw new Error("There is no module " + mod);
  return m;
};
UTRunner.prototype.transport = function(tz) {
  this.zinBch = new JsonBeachhead(this, "fred", this.broker, tz);
  this.broker.beachhead(this.zinBch);
};
UTRunner.prototype.deliver = function(json) {
  this.logger.log("have " + json + " ready for delivery");
  var cx = this.newContext();
  var msgs = this.zinBch.dispatch(cx, json, null);
  this.logger.log("have messages", msgs);
  this.queueMessages(cx, msgs);
};
UTRunner.prototype.addHistory = function(state, title, url) {
};
UTRunner.prototype.runRemote = function(testClz, spec) {
  var cxt = this.newContext();
  var st = new testClz(this, cxt);
  var allSteps = [];
  if (spec.configure) {
    var steps = spec.configure.call(st, cxt);
    for (var j = 0; j < steps.length; j++)
      allSteps.push(steps[j]);
  }
  if (spec.stages) {
    for (var i = 0; i < spec.stages.length; i++) {
      var steps = spec.stages[i].call(st, cxt);
      for (var j = 0; j < steps.length; j++)
        allSteps.push(steps[j]);
    }
  }
  if (spec.cleanup) {
    var steps = spec.cleanup.call(st, cxt);
    for (var j = 0; j < steps.length; j++)
      allSteps.push(steps[j]);
  }
  var bridge = this.logger;
  bridge.executeSync(this, st, cxt, allSteps);
};

// src/main/javascript/unittest/stsecurity.js
function STSecurityModule() {
  this.currentUser = null;
}
STSecurityModule.prototype.requireLogin = function() {
  return this.currentUser != null;
};
STSecurityModule.prototype.userLoggedIn = function(_cxt, app, user) {
  this.currentUser = user;
  app.nowLoggedIn(_cxt);
};
export {
  STSecurityModule,
  UTRunner
};

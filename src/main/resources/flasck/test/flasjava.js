// src/main/javascript/forjava/javalogger.js
var JavaLogger = {};
JavaLogger.log = function() {
  var ret2 = "";
  var sep = "";
  for (var i = 0; i < arguments.length; i++) {
    ret2 += sep + arguments[i];
    sep = " ";
  }
  callJava.log(ret2);
};
JavaLogger.debugmsg = function() {
  var ret2 = "";
  var sep = "";
  for (var i = 0; i < arguments.length; i++) {
    ret2 += sep + arguments[i];
    sep = " ";
  }
  callJava.debugmsg(ret2);
};

// src/main/javascript/runtime/error.js
var FLError2 = class _FLError extends Error {
  constructor(msg) {
    super(msg);
    this.name = "FLError";
  }
  _compare(cx2, other) {
    if (!(other instanceof _FLError))
      return false;
    if (other.message != this.message)
      return false;
    return true;
  }
  _throw() {
    return true;
  }
};
FLError2.eval = function(_cxt, msg) {
  return new FLError2(msg);
};

// src/main/resources/ziwsh.js
var FieldsContainer = function(cx2) {
  this.cx = cx2;
  this.dict = {};
};
FieldsContainer.prototype.set = function(fld, val) {
  this.dict[fld] = val;
};
FieldsContainer.prototype.has = function(fld) {
  return !!this.dict[fld];
};
FieldsContainer.prototype.get = function(fld) {
  const ret2 = this.dict[fld];
  return ret2;
};
FieldsContainer.prototype._compare = function(cx2, other) {
  if (Object.keys(this.dict).length != Object.keys(other.dict).length)
    return false;
  for (var k in this.dict) {
    if (!other.dict.hasOwnProperty(k))
      return false;
    else if (!cx2.compare(this.dict[k], other.dict[k]))
      return false;
  }
  return true;
};
FieldsContainer.prototype.toString = function() {
  return "Fields[" + Object.keys(this.dict).length + "]";
};
var EvalContext = function(env2, broker) {
  this.env = env2;
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
  var ret2 = this.split();
  return ret2;
};
EvalContext.prototype.split = function() {
  var ret2 = this.env.newContext();
  return ret2;
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
EvalContext.prototype.fromWire = function(om2, fields2) {
  var clz = this.env.objects[fields2["_wireable"]];
  if (!clz) {
    throw Error("could not find a registration for " + fields2["_wireable"]);
  }
  return clz.fromWire.call(clz, this, om2, fields2);
};
EvalContext.prototype._bindNamedHandler = function(nh) {
};
var ecxt_default = EvalContext;
var IdempotentHandler = function() {
};
IdempotentHandler.prototype._clz = function() {
  return "org.ziniki.ziwsh.intf.IdempotentHandler";
};
IdempotentHandler.prototype.success = function(cx2) {
};
IdempotentHandler.prototype.failure = function(cx2, msg) {
};
IdempotentHandler.prototype._methods = function() {
  return ["success", "failure"];
};
var LoggingIdempotentHandler = function() {
};
LoggingIdempotentHandler.prototype = new IdempotentHandler();
LoggingIdempotentHandler.prototype.constructor = LoggingIdempotentHandler;
LoggingIdempotentHandler.prototype.success = function(cx2) {
  cx2.log("success");
};
LoggingIdempotentHandler.prototype.failure = function(cx2, msg) {
  cx2.log("failure: " + msg);
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
UnmarshallerDispatcher2.prototype.begin = function(cx2, method) {
  return new DispatcherTraverser(this.svc, method, cx2, new CollectingState(cx2));
};
UnmarshallerDispatcher2.prototype.toString = function() {
  return "ProxyFor[UnmarshallerDispatcher]";
};
var CollectingTraverser = function(cx2, collector) {
  this.cx = cx2;
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
CollectingTraverser.prototype.wireable = function(om2, w) {
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
  const lt = new ListTraverser2(this.cx, this.collector);
  this.collect(lt.ret);
  return lt;
};
CollectingTraverser.prototype.handler = function(cx2, h) {
  this.collect(h);
};
var FieldsTraverser = function(cx2, collector, clz) {
  CollectingTraverser.call(this, cx2, collector);
  var czz = cx2.structNamed(clz);
  if (!czz)
    czz = cx2.objectNamed(clz);
  if (!czz)
    throw new Error("Could not find a definition for " + clz);
  this.creation = new czz(cx2);
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
var ListTraverser2 = function(cx2, collector) {
  CollectingTraverser.call(this, cx2, collector);
  this.ret = [];
};
ListTraverser2.prototype = new CollectingTraverser();
ListTraverser2.prototype.constructor = ListTraverser2;
ListTraverser2.prototype.collect = function(o) {
  this.ret.push(o);
};
ListTraverser2.prototype.complete = function() {
};
var UnmarshalTraverser = function(cx2, collector) {
  ListTraverser2.call(this, cx2, collector);
  this.ret.push(cx2);
};
UnmarshalTraverser.prototype = new ListTraverser2();
UnmarshalTraverser.prototype.constructor = UnmarshalTraverser;
var DispatcherTraverser = function(svc, method, cx2, collector) {
  UnmarshalTraverser.call(this, cx2, collector);
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
  var cx2 = this.ret[0];
  var ih = this.ret[this.ret.length - 1];
  this.ret[0] = cx2 = cx2.env.newContext().bindTo(this.svc);
  try {
    if (ih instanceof NamedIdempotentHandler2) {
      cx2.log("have NIH in dispatch");
    }
    var rets = this.svc[this.method].apply(this.svc, this.ret);
  } catch (e) {
    cx2.log("caught exception and reporting failure", e.toString());
    if (ih instanceof NamedIdempotentHandler2) {
      ih = ih._handler;
    }
    ih.failure(cx2, e.message);
  }
  return rets;
};
var CollectingState = function(cx2) {
  this.cx = cx2;
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
    begin: function(cx2, method) {
      return new JsonArgsMarshaller(broker, { action: "invoke", contract, method, args: [] }, sender, new CollectingState(cx2));
    }
  };
};
JsonBeachhead.prototype.dispatch = function(cx2, json, replyTo) {
  cx2.log("dispatching " + json + " on " + this.name + " and will reply to " + replyTo);
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
  ih.success = function(cx2) {
    sender.send({ action: "idem", method: "success", idem: ihinfo._ihid, args: [] });
  };
  ih.failure = function(cx2, msg) {
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
JsonMarshaller.prototype.wireable = function(om2, w) {
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
JsonMarshaller.prototype.handler = function(cx2, h) {
  var clz, ihid;
  if (h instanceof NamedIdempotentHandler2) {
    cx2.log("have NIH we want clz for", h);
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
var OMWrapper = function(bh, cx2, ux) {
  this.bh = bh;
  this.cx = cx2;
  this.ux = ux;
  this.state = ux.state;
};
OMWrapper.prototype.marshal = function(trav, o) {
  this.bh.handleArg(trav, this.cx, o);
};
var proxy = function(cx2, intf, handler) {
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
    const ret2 = handler["invoke"].call(handler, meth, args);
    return ret2;
  };
};
var MarshallerProxy = function(logger, ctr, svc) {
  this.logger = logger;
  this.svc = svc;
  this.proxy = proxy(logger, ctr, this);
};
MarshallerProxy.prototype.invoke = function(meth, args) {
  const cx2 = args[0];
  try {
    const ux = this.svc.begin(cx2, meth);
    new ArgListMarshaller(this.logger, false, true).marshal(cx2, ux, args);
    return ux.dispatch();
  } catch (e) {
    cx2.log("error during marshalling", e);
    cx2.log("error reported at", e.stack);
    throw e;
  }
};
var ArgListMarshaller = function(logger, includeFirst, includeLast) {
  this.logger = logger;
  this.from = includeFirst ? 0 : 1;
  this.skipEnd = includeLast ? 0 : 1;
};
ArgListMarshaller.prototype.marshal = function(cx2, m, args) {
  const om2 = new ObjectMarshaller(this.logger, m);
  for (var i = this.from; i < args.length - this.skipEnd; i++) {
    om2.marshal(cx2, args[i]);
  }
};
var ObjectMarshaller = function(logger, top) {
  this.logger = logger;
  this.top = top;
};
ObjectMarshaller.prototype.marshal = function(cx2, o) {
  this.recursiveMarshal(cx2, this.top, o);
};
ObjectMarshaller.prototype.recursiveMarshal = function(cx2, ux, o) {
  if (o._throw && o._throw()) {
    cx2.log("throwing because object has _throw");
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
      var ihid = cx2.broker.uniqueHandler(o);
      var intf;
      if (o._clz && o._methods) {
        intf = o;
        cx2.log("identified _clz as", intf._clz());
      } else {
        intf = Object.getPrototypeOf(o);
        var sintf = Object.getPrototypeOf(intf);
        cx2.log("have IH with", intf, "and", sintf);
        if (typeof sintf._methods !== "undefined") {
          intf = sintf;
        }
      }
      var h = new NamedIdempotentHandler2(proxy(cx2, intf, this.makeHandlerInvoker(cx2, ihid)));
      h._ihid = ihid;
      ux.handler(cx2, h);
    } else if (o instanceof NamedIdempotentHandler2) {
      cx2.log("marshalling NIH with", o._ihid, "and", o._name, "and", o._handler.constructor);
      var ihid = cx2.broker.uniqueHandler(o._handler);
      cx2.log("concluded that ihid should be", ihid);
      o._ihid = ihid;
      cx2._bindNamedHandler(o);
      var intf;
      if (o._handler._clz && o._handler._methods) {
        intf = o._handler;
        cx2.log("identified NIH _clz as", intf._clz());
      } else {
        intf = Object.getPrototypeOf(o._handler);
        var sintf = Object.getPrototypeOf(intf);
        cx2.log("have NIH with", intf, "and", sintf);
        if (sintf && typeof sintf._methods !== "undefined") {
          intf = sintf;
        }
      }
      var h = new NamedIdempotentHandler2(proxy(cx2, intf, this.makeHandlerInvoker(cx2, ihid)));
      h._ihid = ihid;
      ux.handler(cx2, h);
    } else if (o.state instanceof FieldsContainer) {
      this.handleStruct(cx2, ux, o);
    } else if (Array.isArray(o)) {
      this.handleArray(cx2, ux, o);
    } else if (o._towire) {
      ux.wireable(this, o);
    } else {
      try {
        cx2.log("o =", JSON.stringify(o));
      } catch (e) {
        cx2.log("could not stringify", o);
      }
      cx2.log("o.state = ", o.state);
      cx2.log("cannot handle object with constructor " + o.constructor.name, new Error().stack);
    }
  } else {
    cx2.log("typeof o =", typeof o);
    try {
      cx2.log("o =", JSON.stringify(o));
    } catch (e) {
      cx2.log("could not stringify", o);
    }
    throw Error("cannot handle " + typeof o);
  }
};
ObjectMarshaller.prototype.handleStruct = function(cx2, ux, o) {
  const fc = o.state;
  if (!fc.has("_type")) {
    throw new Error("No _type defined in " + fc);
  }
  const fm = ux.beginFields(fc.get("_type"));
  ux.circle(o, fm.collectingAs());
  const ks = Object.keys(fc.dict);
  for (var k = 0; k < ks.length; k++) {
    fm.field(ks[k]);
    this.recursiveMarshal(cx2, fm, fc.dict[ks[k]]);
  }
  fm.complete();
};
ObjectMarshaller.prototype.handleArray = function(cx2, ux, l) {
  const ul = ux.beginList();
  for (var k = 0; k < l.length; k++) {
    this.recursiveMarshal(cx2, ul, l[k]);
  }
  ul.complete();
};
ObjectMarshaller.prototype.makeHandlerInvoker = function(cx2, ihid) {
  var broker = cx2.broker;
  var env2 = cx2.env;
  var handler = new Object();
  handler.invoke = function(name, args) {
    var uow = env2.newContext();
    const ih = broker.currentIdem(ihid);
    if (!ih) {
      uow.log("failed to find idem handler for", ihid, new Error().stack);
      return;
    }
    const um = new UnmarshallerDispatcher(null, ih);
    const ux = um.begin(uow, name);
    new ArgListMarshaller(this.logger, false, true).marshal(cx2, ux, args);
    return ux.dispatch();
  };
  return handler;
};
var NoSuchContract = function(ctr) {
  this.ctr = ctr;
};
var isntThere = function(ctr, meth) {
  return function(cx2, ...rest) {
    cx2.log("no such contract for", ctr.name(), meth);
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
    const cx2 = zwc.factory.newContext();
    var actions = this.bh.dispatch(cx2, ev.data, this);
    if (zwc.factory.queueMessages) {
      zwc.factory.queueMessages(cx2, actions);
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
  const ret2 = this.handlers[h];
  if (!ret2) {
    this.logger.log("there is no handler for", h, "in", this.name, "have", JSON.stringify(Object.keys(this.handlers)));
  }
  return ret2;
};
SimpleBroker.prototype.serviceFor = function(h, sf) {
  if (!h._ihid) {
    throw new Error("must have an _ihid");
  }
  this.serviceHandlers.set(h._ihid, sf);
};
SimpleBroker.prototype.cancel = function(cx2, old) {
  const ret2 = this.handlers[old];
  if (!ret2) {
    this.logger.log("there is no handler for", old);
    return;
  }
  delete this.handlers[old];
  this.logger.log("need to cancel " + ret2);
  if (this.serviceHandlers.has(old)) {
    this.serviceHandlers.get(old).cancel(cx2);
    this.serviceHandlers.delete(old);
  }
};
var broker_default = SimpleBroker;

// src/main/javascript/runtime/lists.js
var Nil = function() {
};
Nil.eval = function(_cxt) {
  return [];
};
var Cons = function() {
};
Array.prototype._field_head = function(x) {
  return this[0];
};
Array.prototype._field_head.nfargs = function() {
  return 0;
};
Cons.prototype._field_head = Array.prototype._field_head;
Array.prototype._field_tail = function() {
  return this.slice(1);
};
Array.prototype._field_tail.nfargs = function() {
  return 0;
};
Cons.prototype._field_tail = Array.prototype._field_tail;
Cons.eval = function(_cxt, hd, tl) {
  var cp = _cxt.spine(tl);
  if (cp instanceof FLError2)
    return cp;
  else if (!cp)
    return [hd];
  cp = cp.slice(0);
  cp.splice(0, 0, hd);
  return cp;
};
var AssignItem2 = function(list, n) {
  this.list = list;
  this.n = n;
};
AssignItem2.prototype._field_head = function(_cxt) {
  return this.list[this.n];
};
AssignItem2.prototype._field_head.nfargs = function() {
  return 0;
};
AssignItem2.prototype.set = function(obj) {
  this.list[this.n] = obj;
};

// src/main/javascript/runtime/messages.js
var Debug = function() {
};
Debug.eval = function(_cxt, msg) {
  const d = new Debug();
  d.msg = msg;
  return d;
};
Debug.prototype._compare = function(cx2, other) {
  if (other instanceof Debug) {
    return other.msg == this.msg;
  } else
    return false;
};
Debug.prototype.dispatch = function(cx2) {
  this.msg = cx2.full(this.msg);
  cx2.debugmsg(this.msg);
  return null;
};
Debug.prototype.toString = function() {
  return "Debug[" + this.msg + "]";
};
var Send2 = function() {
};
Send2.eval = function(_cxt, obj, meth, args, handle, subscriptionName) {
  const s = new Send2();
  s.subcontext = _cxt.subcontext;
  if (obj instanceof NamedIdempotentHandler2) {
    s.obj = obj._handler;
  } else {
    s.obj = obj;
  }
  s.meth = meth;
  s.args = args;
  s.handle = handle;
  s.subscriptionName = subscriptionName;
  return s;
};
Send2.prototype._full = function(cx2) {
  this.obj = cx2.full(this.obj);
  this.meth = cx2.full(this.meth);
  this.args = cx2.full(this.args);
  this.handle = cx2.full(this.handle);
  this.subscriptionName = cx2.full(this.subscriptionName);
};
Send2.prototype._compare = function(cx2, other) {
  if (other instanceof Send2) {
    return cx2.compare(this.obj, other.obj) && cx2.compare(this.meth, other.meth) && cx2.compare(this.args, other.args);
  } else
    return false;
};
Send2.prototype.dispatch = function(cx2) {
  this._full(cx2);
  if (this.obj instanceof ResponseWithMessages2) {
    const ret3 = ResponseWithMessages2.messages(cx2, this.obj);
    ret3.push(Send2.eval(cx2, ResponseWithMessages2.response(cx2, this.obj), this.meth, this.args, this.handle));
    return ret3;
  }
  var args = this.args.slice();
  if (this.subcontext) {
    cx2 = cx2.bindTo(this.subcontext);
  } else if (!cx2.subcontext) {
    cx2 = cx2.bindTo(this.obj);
  }
  args.splice(0, 0, cx2);
  var hdlr;
  if (this.handle) {
    hdlr = new NamedIdempotentHandler2(this.handle, this.subscriptionName);
  } else {
    hdlr = new IdempotentHandler();
  }
  args.splice(args.length, 0, hdlr);
  var meth = this.obj._methods()[this.meth];
  if (!meth)
    return;
  var ret2 = meth.apply(this.obj, args);
  return ret2;
};
Send2.prototype.toString = function() {
  return "Send[" + this.obj + ":" + this.meth + "]";
};
var Assign = function() {
};
Assign.eval = function(_cxt, obj, slot, expr) {
  const s = new Assign();
  s.obj = obj;
  s.slot = slot;
  s.expr = expr;
  return s;
};
Assign.prototype._full = function(cx2) {
  this.obj = cx2.full(this.obj);
  this.slot = cx2.full(this.slot);
  this.expr = cx2.full(this.expr);
};
Assign.prototype._compare = function(cx2, other) {
  if (other instanceof Assign) {
    return cx2.compare(this.obj, other.obj) && cx2.compare(this.slot, other.slot) && cx2.compare(this.expr, other.expr);
  } else
    return false;
};
Assign.prototype.dispatch = function(cx2) {
  var msgs = [];
  var target = this.obj;
  if (target.dispatch) {
    var rwm = this.obj.dispatch(cx2);
    target = rwm;
  }
  if (this.expr instanceof ResponseWithMessages2) {
    msgs.unshift(ResponseWithMessages2.messages(cx2, this.expr));
    this.expr = ResponseWithMessages2.response(cx2, this.expr);
  }
  target.state.set(this.slot, this.expr);
  if (this.obj._updateDisplay)
    cx2.env.queueMessages(cx2, new UpdateDisplay(cx2, this.obj));
  else if (this.obj._card && this.obj._card._updateDisplay)
    cx2.env.queueMessages(cx2, new UpdateDisplay(cx2, this.obj._card));
  return msgs;
};
Assign.prototype.toString = function() {
  return "Assign[]";
};
var AssignCons = function() {
};
AssignCons.eval = function(_cxt, obj, expr) {
  const s = new AssignCons();
  s.obj = obj;
  s.expr = expr;
  return s;
};
AssignCons.prototype._full = function(cx2) {
  this.obj = cx2.full(this.obj);
  this.expr = cx2.full(this.expr);
};
AssignCons.prototype._compare = function(cx2, other) {
  if (other instanceof AssignCons) {
    return cx2.compare(this.obj, other.obj) && cx2.compare(this.expr, other.expr);
  } else
    return false;
};
AssignCons.prototype.dispatch = function(cx2) {
  var msgs = [];
  var target = this.obj;
  if (target.dispatch) {
    var rwm = this.obj.dispatch(cx2);
    target = rwm;
  }
  if (target instanceof FLError) {
    cx2.log(target);
    return;
  }
  if (!(target instanceof AssignItem2)) {
    throw Error("No, it needs to be an Item");
  }
  if (this.expr instanceof ResponseWithMessages2) {
    msgs.unshift(ResponseWithMessages2.messages(cx2, this.expr));
    this.expr = ResponseWithMessages2.response(cx2, this.expr);
  }
  target.set(this.expr);
  return msgs;
};
AssignCons.prototype.toString = function() {
  return "AssignCons[]";
};
var ResponseWithMessages2 = function(cx2, obj, msgs) {
  this.obj = obj;
  this.msgs = msgs;
};
ResponseWithMessages2.prototype._full = function(cx2) {
  this.obj = cx2.full(this.obj);
  this.msgs = cx2.full(this.msgs);
};
ResponseWithMessages2.response = function(cx2, rwm) {
  if (rwm instanceof ResponseWithMessages2)
    return rwm.obj;
  else
    return rwm;
};
ResponseWithMessages2.messages = function(cx2, rwm) {
  if (rwm instanceof ResponseWithMessages2)
    return rwm.msgs;
  else
    return null;
};
ResponseWithMessages2.prototype.toString = function() {
  return "ResponseWithMessages (" + this.obj + ")";
};
var UpdateDisplay = function(cx2, card) {
  this.card = card;
};
UpdateDisplay.prototype._compare = function(cx2, other) {
  if (other instanceof UpdateDisplay) {
    return this.card == other.card || this.card == null || other.card == null;
  } else
    return false;
};
UpdateDisplay.eval = function(cx2) {
  return new UpdateDisplay(cx2, null);
};
UpdateDisplay.prototype.dispatch = function(cx2) {
  if (this.card._updateDisplay)
    cx2.needsUpdate(this.card);
};
UpdateDisplay.prototype.toString = function() {
  return "UpdateDisplay";
};

// src/main/javascript/runtime/closure.js
var FLClosure = function(obj, fn, args) {
  if (!fn)
    throw new Error("must define a function");
  this.obj = obj;
  this.fn = fn;
  args.splice(0, 0, null);
  this.args = args;
};
FLClosure.prototype.splitRWM = function(msgsTo) {
  this.msgsTo = msgsTo;
};
FLClosure.prototype.eval = function(_cxt) {
  if (this.val)
    return this.val;
  this.args[0] = _cxt;
  this.obj = _cxt.full(this.obj);
  if (this.obj instanceof FLError2)
    return this.obj;
  if (this.fn instanceof FLError2)
    return this.fn;
  var cnt = this.fn.nfargs();
  this.val = this.fn.apply(this.obj, this.args.slice(0, cnt + 1));
  if (typeof this.msgsTo !== "undefined") {
    if (this.val instanceof ResponseWithMessages2) {
      _cxt.addAll(this.msgsTo, ResponseWithMessages2.messages(_cxt, this.val));
      this.val = ResponseWithMessages2.response(_cxt, this.val);
    } else if (this.val instanceof FLClosure) {
      this.val.splitRWM(this.msgsTo);
    }
  }
  if (cnt + 1 < this.args.length) {
    this.val = new FLClosure(this.obj, this.val, this.args.slice(cnt + 1));
  }
  return this.val;
};
FLClosure.prototype.apply = function(_, args) {
  const asfn = this.eval(args[0]);
  return asfn.apply(null, args);
};
FLClosure.prototype.nfargs = function() {
  return 0;
};
FLClosure.prototype.toString = function() {
  return "FLClosure[]";
};
var closure_default = FLClosure;

// src/main/javascript/runtime/curry.js
var FLCurry = function(obj, fn, reqd, xcs) {
  if (fn == null)
    throw Error("fn cannot be null");
  this.obj = obj;
  this.fn = fn;
  this.xcs = xcs;
  this.reqd = reqd;
  this.missing = [];
  for (var i = 1; i <= reqd; i++) {
    if (!(i in xcs))
      this.missing.push(i);
  }
};
FLCurry.prototype.apply = function(_, args) {
  var _cxt = args[0];
  if (args.length == 1)
    return this;
  if (args.length - 1 == this.missing.length) {
    var as = [_cxt];
    var from = 1;
    for (var i = 1; i <= this.reqd; i++) {
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
    for (var i = 1; i < args.length; i++) {
      var m = miss.pop();
      xcs[m] = args[i];
    }
    return new FLCurry(this.obj, this.fn, this.reqd, xcs);
  }
};
FLCurry.prototype.nfargs = function() {
  return this.reqd;
};
FLCurry.prototype.toString = function() {
  return "FLCurry[" + this.reqd + "]";
};
var curry_default = FLCurry;

// src/main/javascript/runtime/makesend.js
var FLMakeSend = function(meth, obj, nargs, handler, subscriptionName) {
  this.meth = meth;
  this.obj = obj;
  this.nargs = nargs;
  this.current = [];
  this.handler = handler;
  this.subscriptionName = subscriptionName;
};
FLMakeSend.prototype.apply = function(obj, args) {
  var cx2 = args[0];
  var all = this.current.slice();
  for (var i = 1; i < args.length; i++)
    all.push(args[i]);
  if (all.length == this.nargs) {
    return Send2.eval(cx2, this.obj, this.meth, all, this.handler, this.subscriptionName);
  } else {
    var ret2 = new FLMakeSend(this.meth, this.obj, this.nargs, this.handler, this.subscriptionName);
    ret2.current = all;
    return ret2;
  }
};
FLMakeSend.prototype.nfargs = function() {
  return this.nargs;
};
FLMakeSend.prototype.toString = function() {
  return "MakeSend[" + this.nargs + "]";
};
var makesend_default = FLMakeSend;

// src/main/javascript/runtime/events.js
var FLEvent = function() {
};
var FLEventSourceTrait = function(elt, source) {
  this.elt = elt;
  this.source = source;
};
FLEvent.prototype._eventSource = function(cx2, tih) {
  return this.EventSource.source;
};
FLEvent.prototype._methods = function() {
  return {
    _eventSource: FLEvent.prototype._eventSource
  };
};
var ClickEvent = function() {
};
ClickEvent.prototype = new FLEvent();
ClickEvent.prototype.constructor = ClickEvent;
ClickEvent._eventName = "click";
ClickEvent.eval = function(cx2) {
  return new ClickEvent();
};
ClickEvent.prototype._areYouA = function(cx2, name) {
  return name == "ClickEvent" || name == "Event";
};
ClickEvent.prototype._makeJSEvent = function(_cxt, div) {
  const ev = new Event("click", { bubbles: true });
  return ev;
};
ClickEvent.prototype._field_source = function(_cxt, ev) {
  return this.EventSource.source;
};
ClickEvent.prototype._field_source.nfargs = function() {
  return 0;
};
var ScrollTo = function(st) {
  this.st = st;
};
ScrollTo.prototype = new FLEvent();
ScrollTo.prototype.constructor = ScrollTo;
ScrollTo._eventName = "scrollTo";
ScrollTo.eval = function(cx2, st) {
  return new ScrollTo(st);
};
ScrollTo.prototype._areYouA = function(cx2, name) {
  return name == "ScrollTo" || name == "Event";
};
ScrollTo.prototype._makeJSEvent = function(_cxt, div) {
  div.scrollTop = this.st;
  const ev = new Event("scroll", { bubbles: true });
  return ev;
};
ScrollTo.prototype._field_source = function(_cxt, ev) {
  return this.EventSource.source;
};
ScrollTo.prototype._field_source.nfargs = function() {
  return 0;
};

// src/main/javascript/runtime/flcxt.js
var FLContext = function(env2, broker) {
  ecxt_default.call(this, env2, broker);
  this.subcontext = null;
};
FLContext.prototype = new ecxt_default();
FLContext.prototype.constructor = FLContext;
FLContext.prototype.bindTo = function(to) {
  var ret2 = this.split();
  ret2.subcontext = to;
  return ret2;
};
FLContext.prototype.split = function() {
  var ret2 = this.env.newContext();
  ret2.subcontext = this.subcontext;
  return ret2;
};
FLContext.prototype.addAll = function(ret2, arr) {
  this.env.addAll(ret2, arr);
};
FLContext.prototype.closure = function(fn, ...args) {
  return new closure_default(null, fn, args);
};
FLContext.prototype.oclosure = function(fn, obj, ...args) {
  return new closure_default(obj, fn, args);
};
FLContext.prototype.curry = function(reqd, fn, ...args) {
  var xcs = {};
  for (var i = 0; i < args.length; i++) {
    xcs[i + 1] = args[i];
  }
  return new curry_default(null, fn, reqd, xcs);
};
FLContext.prototype.ocurry = function(reqd, fn, obj, ...args) {
  var xcs = {};
  for (var i = 0; i < args.length; i++) {
    xcs[i + 1] = args[i];
  }
  return new curry_default(obj, fn, reqd, xcs);
};
FLContext.prototype.xcurry = function(reqd, ...args) {
  var fn;
  var xcs = {};
  for (var i = 0; i < args.length; i += 2) {
    if (args[i] == 0)
      fn = args[i + 1];
    else
      xcs[args[i]] = args[i + 1];
  }
  return new curry_default(null, fn, reqd, xcs);
};
FLContext.prototype.array = function(...args) {
  return args;
};
FLContext.prototype.makeTuple = function(...args) {
  return Tuple.eval(this, args);
};
FLContext.prototype.hash = function(...args) {
  var ret2 = {};
  for (var i = 0; i < args.length; i++) {
    var hp = this.head(args[i]);
    if (!(hp instanceof HashPair))
      return new FLError2("member was not a hashpair");
    var m = this.full(hp.m);
    ret2[m] = hp.o;
  }
  return ret2;
};
FLContext.prototype.applyhash = function(basic, hash) {
  basic = this.head(basic);
  if (basic instanceof FLError2)
    return basic;
  hash = this.spine(hash);
  if (hash instanceof FLError2)
    return hash;
  var okh = Object.keys(hash);
  for (var i = 0; i < okh.length; i++) {
    var p = okh[i];
    if (!basic.state.has(p))
      return new FLError2("cannot override member: " + p);
    basic.state.set(p, hash[p]);
  }
  return basic;
};
FLContext.prototype.tupleMember = function(tuple, which) {
  tuple = this.head(tuple);
  if (!(tuple instanceof Tuple))
    throw "not a tuple: " + tuple;
  return tuple.args[which];
};
FLContext.prototype.error = function(msg) {
  return FLError2.eval(this, msg);
};
FLContext.prototype.mksend = function(meth, obj, cnt, handler, subscriptionName) {
  if (cnt == 0)
    return Send2.eval(this, obj, meth, [], handler, subscriptionName);
  else
    return new makesend_default(meth, obj, cnt, handler, subscriptionName);
};
FLContext.returnNull = function(_cxt) {
  return null;
};
FLContext.prototype.mkacor = function(meth, obj, cnt) {
  if (cnt == 0) {
    if (typeof obj === "undefined" || obj === null)
      return obj;
    else
      return this.oclosure(meth, obj);
  } else {
    if (typeof obj === "undefined" || obj === null) {
      var fn = function(_cxt) {
        return null;
      };
      fn.nfargs = function() {
        return cnt;
      };
      return this.ocurry(cnt, fn, obj);
    } else
      return this.ocurry(cnt, meth, obj);
  }
};
FLContext.prototype.makeStatic = function(clz, meth) {
  const oc = this.objectNamed(clz);
  const ocm = oc[meth];
  const ret2 = function(...args) {
    return ocm.apply(null, args);
  };
  ret2.nfargs = ocm.nfargs;
  return ret2;
};
FLContext.prototype.head = function(obj) {
  while (obj instanceof closure_default)
    obj = obj.eval(this);
  return obj;
};
FLContext.prototype.spine = function(obj) {
  obj = this.head(obj);
  if (obj instanceof FLError2)
    return obj;
  if (!obj)
    return [];
  if (Array.isArray(obj))
    return obj;
  if (obj.constructor === Object) {
    return obj;
  }
  throw Error("spine should only be called on lists");
};
FLContext.prototype.full = function(obj) {
  var msgs = [];
  obj = this.head(obj);
  if (obj == null) {
  } else if (obj._full) {
    obj._full(this);
  } else if (Array.isArray(obj)) {
    for (var i = 0; i < obj.length; i++) {
      obj[i] = this.full(obj[i]);
      if (obj[i] instanceof ResponseWithMessages2) {
        msgs.unshift(obj[i].msgs);
        obj[i] = obj[i].obj;
      }
    }
  } else if (obj.state instanceof FieldsContainer) {
    var ks = Object.keys(obj.state.dict);
    for (var i = 0; i < ks.length; i++) {
      var tmp = this.full(obj.state.dict[ks[i]]);
      if (tmp instanceof ResponseWithMessages2) {
        msgs.unshift(tmp.msgs);
        tmp = tmp.obj;
      }
      obj.state.dict[ks[i]] = tmp;
    }
  }
  if (msgs.length)
    return new ResponseWithMessages2(this, obj, msgs);
  else
    return obj;
};
FLContext.prototype.isTruthy = function(val) {
  val = this.full(val);
  return !!val;
};
FLContext.prototype.isA = function(val, ty) {
  if (val instanceof Object && "_areYouA" in val) {
    return val._areYouA(this, ty);
  }
  switch (ty) {
    case "Any":
      return true;
    case "Boolean":
      return val === true || val === false;
    case "True":
      return val === true;
    case "False":
      return val === false;
    case "Number":
      return typeof val == "number";
    case "String":
      return typeof val == "string";
    case "List":
      return Array.isArray(val);
    case "Nil":
      return Array.isArray(val) && val.length == 0;
    case "Cons":
      return Array.isArray(val) && val.length > 0;
    default:
      return false;
  }
};
FLContext.prototype.compare = function(left, right) {
  if (typeof left === "number" || typeof left === "string") {
    return left === right;
  } else if (Array.isArray(left) && Array.isArray(right)) {
    if (left.length !== right.length)
      return false;
    for (var i = 0; i < left.length; i++) {
      if (!this.compare(left[i], right[i]))
        return false;
    }
    return true;
  } else if (left instanceof FLError2 && right instanceof FLError2) {
    return left.message === right.message;
  } else if (left._compare) {
    return left._compare(this, right);
  } else if (left.state && right.state && left.state instanceof FieldsContainer && right.state instanceof FieldsContainer && left.name && right.name && left.name() === right.name()) {
    return left.state._compare(this, right.state);
  } else if (left.state && right.state && left.state instanceof FieldsContainer && right.state instanceof FieldsContainer && left.state.get("_type") && right.state.get("_type") && left.state.get("_type") === right.state.get("_type")) {
    return left.state._compare(this, right.state);
  } else
    return left == right;
};
FLContext.prototype.field = function(obj, field) {
  obj = this.full(obj);
  if (Array.isArray(obj)) {
    if (field == "head") {
      if (obj.length > 0)
        return obj[0];
      else
        return this.error("head(nil)");
    } else if (field == "tail") {
      if (obj.length > 0)
        return obj.slice(1);
      else
        return this.error("tail(nil)");
    } else
      return this.error('no function "' + field + "'");
  } else {
    return obj.state.get(field);
  }
};
FLContext.prototype.getSingleton = function(name) {
  return this.env.getSingleton(name);
};
FLContext.prototype.cacheSingleton = function(name, value) {
  this.env.cacheSingleton(name, value);
};
FLContext.prototype.nextDocumentId = function() {
  return "flaselt_" + this.env.nextDivId++;
};
FLContext.prototype.attachEventToCard = function(card, handlerInfo, div, wrapper) {
  const eventName = handlerInfo.event._eventName;
  if (div) {
    var id1 = this.env.evid++;
    var handler = (ev) => {
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
};
FLContext.prototype.handleEvent = function(card, handler, event) {
  if (card && card._updateFromInputs) {
    card._updateFromInputs();
  }
  var reply = [];
  if (handler) {
    reply = handler.call(card, this, event);
  }
  if (reply instanceof FLError2) {
    this.log(reply.message);
    return;
  }
  reply.push(new UpdateDisplay(this, card));
  this.env.queueMessages(this, reply);
};
FLContext.prototype.localCard = function(cardClz, eltName) {
  const self = this;
  const elt = document.getElementById(eltName);
  const card = new cardClz(this);
  if (this.broker.awaitingServerConnection()) {
    const login = document.createElement("button");
    login.innerText = "Log In";
    login.onclick = function() {
      console.log("want to log in");
      window.haveit = function(token, secret) {
        console.log("have credentials:", token, secret);
        env.broker.updateConnection(zinikiServer + "?token=" + token + "&secret=" + secret);
        elt.innerHTML = "";
        self.startCard(card, elt);
      };
      window.open(zinikiLogin);
    };
    elt.appendChild(login);
  } else {
    this.startCard(card, elt);
  }
  return card;
};
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
};
FLContext.prototype.findContractOnCard = function(card, ctr) {
  for (var ce in Object.getOwnPropertyDescriptors(card._contracts)) {
    if (card._contracts[ce][ctr])
      return card._contracts[ce][ctr];
  }
};
FLContext.prototype.needsUpdate = function(card) {
  if (typeof this.updateCards === "undefined")
    throw Error("cannot update when not in event loop");
  if (!this.updateCards.includes(card))
    this.updateCards.push(card);
};
FLContext.prototype.newdiv = function(cnt) {
  this.env.newdiv(cnt);
};
FLContext.prototype.expectCancel = function(ctr) {
  this.env.expectCancel(ctr);
};
FLContext.prototype.assertSatisfied = function() {
  this.env.assertSatisfied();
};
FLContext.prototype.show = function(val) {
  if (val === true)
    return "True";
  else if (val === false)
    return "False";
  return "" + val;
};
FLContext.prototype.log = function(...args) {
  this.env.logger.log.apply(this.env.logger, args);
};
FLContext.prototype.addHistory = function(state, title, url) {
  this.env.addHistory(state, title, url);
};
FLContext.prototype._bindNamedHandler = function(nh) {
  if (!this.subcontext) {
    this.log("no sub context", new Error().stack);
    throw new Error("sub context not bound");
  }
  if (nh._handler) {
    var de;
    if (this.env.subDag.has(this.subcontext)) {
      de = this.env.subDag.get(this.subcontext);
    } else {
      de = [];
      this.env.subDag.set(this.subcontext, de);
    }
    if (!de.includes(nh._handler)) {
      de.push(nh._handler);
    }
  }
  if (!nh._name) {
    var forcxt = this.env.unnamedSubscriptions.get(this.subcontext);
    if (!forcxt) {
      forcxt = [];
      this.env.unnamedSubscriptions.set(this.subcontext, forcxt);
    }
    forcxt.push(nh);
  } else {
    var forcxt = this.env.namedSubscriptions.get(this.subcontext);
    if (!forcxt) {
      forcxt = /* @__PURE__ */ new Map();
      this.env.namedSubscriptions.set(this.subcontext, forcxt);
    }
    if (forcxt.has(nh._name)) {
      var old = forcxt.get(nh._name);
      var ns2 = this.env.subDag.get(old._handler);
      this.log("ns =", ns2);
      if (ns2) {
        ns2.forEach((sc) => {
          this.unsubscribeAll(sc);
        });
      }
      this.env.broker.cancel(this, old._ihid);
    }
    forcxt.set(nh._name, nh);
  }
};
FLContext.prototype.unsubscribeAll = function(card) {
  this.env.unsubscribeAll(this, card);
};

// src/main/javascript/runtime/builtin.js
var True = function() {
};
True.eval = function(_cxt) {
  return true;
};
var False = function() {
};
False.eval = function(_cxt) {
  return false;
};
var Tuple2 = function() {
};
Tuple2.eval = function(_cxt, args) {
  const ret2 = new Tuple2();
  ret2.args = args;
  return ret2;
};
var TypeOf = function(ty) {
  this.ty = ty;
};
TypeOf.eval = function(_cxt, expr) {
  expr = _cxt.full(expr);
  if (typeof expr == "object")
    return new TypeOf(expr.constructor.name);
  else
    return new TypeOf(typeof expr);
};
TypeOf.prototype._compare = function(_cxt, other) {
  if (other instanceof TypeOf) {
    return this.ty == other.ty;
  } else
    return false;
};
TypeOf.prototype.toString = function() {
  if (this.ty._typename) {
    return this.ty._typename;
  }
  switch (this.ty) {
    case "number":
      return "Number";
    case "string":
      return "String";
    case "TypeOf":
      return "Type";
    default:
      return this.ty;
  }
};
TypeOf.prototype._towire = function(wf) {
  wf.type = this.toString();
  wf._wireable = "org.flasck.jvm.builtin.TypeOf";
};
var FLBuiltin = function() {
};
FLBuiltin.arr_length = function(_cxt, arr) {
  arr = _cxt.head(arr);
  if (!Array.isArray(arr))
    return _cxt.error("not an array");
  return arr.length;
};
FLBuiltin.arr_length.nfargs = function() {
  return 1;
};
FLBuiltin.plus = function(_cxt, a, b) {
  a = _cxt.full(a);
  b = _cxt.full(b);
  return a + b;
};
FLBuiltin.plus.nfargs = function() {
  return 2;
};
FLBuiltin.minus = function(_cxt, a, b) {
  a = _cxt.full(a);
  b = _cxt.full(b);
  return a - b;
};
FLBuiltin.minus.nfargs = function() {
  return 2;
};
FLBuiltin.unaryMinus = function(_cxt, a) {
  a = _cxt.full(a);
  return -a;
};
FLBuiltin.unaryMinus.nfargs = function() {
  return 1;
};
FLBuiltin.mul = function(_cxt, a, b) {
  a = _cxt.full(a);
  b = _cxt.full(b);
  return a * b;
};
FLBuiltin.mul.nfargs = function() {
  return 2;
};
FLBuiltin.div = function(_cxt, a, b) {
  a = _cxt.full(a);
  b = _cxt.full(b);
  return a / b;
};
FLBuiltin.div.nfargs = function() {
  return 2;
};
FLBuiltin.mod = function(_cxt, a, b) {
  a = _cxt.full(a);
  b = _cxt.full(b);
  return a % b;
};
FLBuiltin.mod.nfargs = function() {
  return 2;
};
FLBuiltin.not = function(_cxt, a) {
  a = _cxt.full(a);
  return !a;
};
FLBuiltin.not.nfargs = function() {
  return 1;
};
FLBuiltin.boolAnd = function(_cxt, a, b) {
  a = _cxt.full(a);
  b = _cxt.full(b);
  return _cxt.isTruthy(a) && _cxt.isTruthy(b);
};
FLBuiltin.boolAnd.nfargs = function() {
  return 2;
};
FLBuiltin.boolOr = function(_cxt, a, b) {
  a = _cxt.full(a);
  b = _cxt.full(b);
  return _cxt.isTruthy(a) || _cxt.isTruthy(b);
};
FLBuiltin.boolOr.nfargs = function() {
  return 2;
};
FLBuiltin.concat = function(_cxt, a, b) {
  a = _cxt.full(a);
  if (!a)
    a = "";
  b = _cxt.full(b);
  if (!b)
    b = "";
  return a + b;
};
FLBuiltin.concat.nfargs = function() {
  return 2;
};
FLBuiltin.nth = function(_cxt, n, list) {
  n = _cxt.full(n);
  if (typeof n != "number")
    return new FLError2("no matching case");
  list = _cxt.spine(list);
  if (!Array.isArray(list))
    return new FLError2("no matching case");
  if (n < 0 || n >= list.length)
    return new FLError2("out of bounds");
  return list[n];
};
FLBuiltin.nth.nfargs = function() {
  return 2;
};
FLBuiltin.item = function(_cxt, n, list) {
  n = _cxt.full(n);
  if (typeof n != "number")
    return new FLError2("no matching case");
  list = _cxt.spine(list);
  if (!Array.isArray(list))
    return new FLError2("no matching case");
  if (n < 0 || n >= list.length)
    return new FLError2("out of bounds");
  return new AssignItem(list, n);
};
FLBuiltin.item.nfargs = function() {
  return 2;
};
FLBuiltin.append = function(_cxt, list, elt) {
  list = _cxt.spine(list);
  if (!Array.isArray(list))
    return new FLError2("no matching case");
  var cp = list.slice(0);
  cp.push(elt);
  return cp;
};
FLBuiltin.append.nfargs = function() {
  return 2;
};
FLBuiltin.replace = function(_cxt, list, n, elt) {
  n = _cxt.full(n);
  if (typeof n != "number")
    return new FLError2("no matching case");
  list = _cxt.spine(list);
  if (!Array.isArray(list))
    return new FLError2("no matching case");
  if (n < 0 || n >= list.length)
    return new FLError2("out of bounds");
  var cp = list.slice(0);
  cp[n] = elt;
  return cp;
};
FLBuiltin.replace.nfargs = function() {
  return 3;
};
FLBuiltin.concatLists = function(_cxt, list) {
  list = _cxt.spine(list);
  var ret2 = [];
  for (var i = 0; i < list.length; i++) {
    var li = _cxt.spine(list[i]);
    for (var j = 0; j < li.length; j++) {
      ret2.push(li[j]);
    }
  }
  return ret2;
};
FLBuiltin.concatLists.nfargs = function() {
  return 1;
};
FLBuiltin.take = function(_cxt, quant, list) {
  list = _cxt.spine(list);
  if (list instanceof FLError2)
    return list;
  else if (!list)
    return [];
  quant = _cxt.full(quant);
  if (quant instanceof FLError2)
    return quant;
  if (typeof quant !== "number")
    return new FLError2("no matching case");
  if (list.length <= quant)
    return list;
  return list.slice(0, quant);
};
FLBuiltin.take.nfargs = function() {
  return 2;
};
FLBuiltin.drop = function(_cxt, quant, list) {
  list = _cxt.spine(list);
  if (list instanceof FLError2)
    return list;
  else if (!list)
    return [];
  quant = _cxt.full(quant);
  if (quant instanceof FLError2)
    return quant;
  if (typeof quant !== "number")
    return new FLError2("no matching case");
  return list.slice(quant);
};
FLBuiltin.drop.nfargs = function() {
  return 2;
};
FLBuiltin.concatMany = function(_cxt, rest) {
  var ret2 = "";
  for (var i = 0; i < rest.length; i++) {
    var tmp = _cxt.full(rest[i]);
    if (!tmp)
      continue;
    if (ret2.length > 0)
      ret2 += " ";
    ret2 += tmp;
  }
  return ret2;
};
FLBuiltin.concatMany.nfargs = function() {
  return 1;
};
FLBuiltin.strlen = function(_cxt, str) {
  str = _cxt.head(str);
  if (typeof str != "string")
    return _cxt.error("not a string");
  return str.length;
};
FLBuiltin.strlen.nfargs = function() {
  return 1;
};
FLBuiltin.isNull = function(_cxt, a) {
  a = _cxt.full(a);
  return a == null || a == void 0;
};
FLBuiltin.isNull.nfargs = function() {
  return 1;
};
FLBuiltin.isEqual = function(_cxt, a, b) {
  a = _cxt.full(a);
  b = _cxt.full(b);
  return _cxt.compare(a, b);
};
FLBuiltin.isEqual.nfargs = function() {
  return 2;
};
FLBuiltin.isNotEqual = function(_cxt, a, b) {
  a = _cxt.full(a);
  b = _cxt.full(b);
  return a != b;
};
FLBuiltin.isNotEqual.nfargs = function() {
  return 2;
};
FLBuiltin.greaterEqual = function(_cxt, a, b) {
  a = _cxt.full(a);
  b = _cxt.full(b);
  return a >= b;
};
FLBuiltin.greaterEqual.nfargs = function() {
  return 2;
};
FLBuiltin.greaterThan = function(_cxt, a, b) {
  a = _cxt.full(a);
  b = _cxt.full(b);
  return a > b;
};
FLBuiltin.greaterThan.nfargs = function() {
  return 2;
};
FLBuiltin.lessEqual = function(_cxt, a, b) {
  a = _cxt.full(a);
  b = _cxt.full(b);
  return a <= b;
};
FLBuiltin.lessEqual.nfargs = function() {
  return 2;
};
FLBuiltin.lessThan = function(_cxt, a, b) {
  a = _cxt.full(a);
  b = _cxt.full(b);
  return a < b;
};
FLBuiltin.lessThan.nfargs = function() {
  return 2;
};
FLBuiltin._probe_state = function(_cxt, mock, v) {
  var sh = _cxt.full(mock);
  if (sh instanceof FLError2)
    return sh;
  else if (sh.routes) {
    if (sh.routes[v] === void 0)
      return new FLError2("there is no card bound to route var '" + v + "'");
    return sh.routes[v];
  } else if (sh.card) {
    sh = sh.card;
    sh._updateFromInputs();
  } else if (sh.agent)
    sh = sh.agent;
  if (sh.state.dict[v] === void 0)
    return new FLError2("No field '" + v + "' in probe_state");
  return sh.state.dict[v];
};
FLBuiltin._probe_state.nfargs = function() {
  return 2;
};
FLBuiltin._underlying = function(_cxt, mock) {
  return mock._underlying(_cxt);
};
FLBuiltin._underlying.nfargs = function() {
  return 1;
};
FLBuiltin.dispatch = function(_cxt, msgs) {
  msgs = _cxt.full(msgs);
  if (msgs instanceof FLError2)
    return msgs;
  var ret2 = [];
  var te = _cxt.env;
  te.queueMessages = function(cx2, m) {
    ret2.push(m);
  };
  _cxt.env.handleMessages(_cxt, msgs);
  return ret2;
};
FLBuiltin.dispatch.nfargs = function() {
  return 1;
};
FLBuiltin.show = function(_cxt, val) {
  val = _cxt.full(val);
  return _cxt.show(val);
};
FLBuiltin.show.nfargs = function() {
  return 1;
};
FLBuiltin.expr = function(_cxt, val) {
  return _cxt.show(val);
};
FLBuiltin.expr.nfargs = function() {
  return 1;
};
var MakeHash = function() {
};
MakeHash.eval = function(_cxt, args) {
  throw Error("should not be called - optimize away");
};
var HashPair2 = function() {
};
HashPair2.eval = function(_cxt, args) {
  var ret2 = new HashPair2();
  ret2.m = args[0];
  ret2.o = args[1];
  return ret2;
};
FLBuiltin.hashPair = function(_cxt, key, value) {
  return HashPair2.eval(_cxt, [key, value]);
};
FLBuiltin.hashPair.nfargs = function() {
  return 2;
};
FLBuiltin.assoc = function(_cxt, hash, member) {
  hash = _cxt.spine(hash);
  member = _cxt.full(member);
  if (hash[member])
    return hash[member];
  else
    return new FLError2("no member " + member);
};
FLBuiltin.assoc.nfargs = function() {
  return 2;
};
function FLURI2(s) {
  this.uri = s;
}
FLURI2.prototype.resolve = function(base) {
  return new URL(this.uri, base);
};
FLURI2.prototype._towire = function(into) {
  into.uri = this.uri;
};
FLBuiltin.parseUri = function(_cxt, s) {
  s = _cxt.full(s);
  if (s instanceof FLError2)
    return s;
  else if (typeof s !== "string")
    return new FLError2("not a string");
  else
    return new FLURI2(s);
};
FLBuiltin.parseUri.nfargs = function() {
  return 1;
};
FLBuiltin.parseJson = function(_cxt, s) {
  s = _cxt.full(s);
  if (s instanceof FLError2)
    return s;
  return JSON.parse(s);
};
FLBuiltin.parseJson.nfargs = function() {
  return 1;
};

// src/main/javascript/runtime/repeater.js
var ContainerRepeater = function() {
};
ContainerRepeater.prototype.callMe = function(cx2, callback) {
  return Send2.eval(cx2, callback, "call", []);
};

// src/main/javascript/runtime/html.js
var Html = function(_cxt, _html) {
  object_default.call(this, _cxt);
  this.state = _cxt.fields();
  this.state.set("html", _html);
};
Html._ctor_from = function(_cxt, _card, _html) {
  var ret2;
  if (!(_html instanceof AjaxMessage)) {
    ret2 = new FLError2("not an AjaxMessage");
  } else {
    ret2 = new Html(_cxt, _html.state.get("body"));
  }
  return new ResponseWithMessages2(_cxt, ret2, []);
};
Html._ctor_from.nfargs = function() {
  return 2;
};
Html.prototype._compare = function(_cxt, other) {
  if (!(other instanceof Html))
    return false;
  return this.state.get("html").toString() == other.state.get("html").toString();
};
Html.prototype.toString = function() {
  return "Html";
};

// src/main/javascript/runtime/card.js
var FLCard = function(cx2) {
  this._renderTree = null;
  this._containedIn = null;
};
FLCard.prototype._renderInto = function(_cxt, div) {
  this._containedIn = div;
  div.innerHTML = "";
  if (this._template) {
    this._renderTree = {};
    var t = document.getElementById(this._template);
    if (t != null) {
      var cloned = t.content.cloneNode(true);
      var ncid = _cxt.nextDocumentId();
      cloned.firstElementChild.id = ncid;
      this._renderTree["_id"] = ncid;
      div.appendChild(cloned);
      this._updateDisplay(_cxt, this._renderTree);
      this._resizeDisplayElements(_cxt, this._renderTree);
    }
  }
  if (this._eventHandlers) {
    this._attachHandlers(_cxt, this._renderTree, div, "_", null, 1, this);
  }
};
FLCard.prototype._resizeDisplayElements = function(_cxt, _rt) {
  if (!_rt)
    return;
  var container = document.getElementById(_rt._id);
  var cw = container.clientWidth;
  var ch = container.clientHeight;
  var nodes = document.querySelectorAll("#" + _rt._id + " .flas-sizing");
  for (var i = 0; i < nodes.length; i++) {
    var n = nodes[i];
    var cl = n.classList;
    for (var j = 0; j < cl.length; j++) {
      var cle = cl[j];
      if (cle.startsWith("flas-sizing-")) {
        this._setSizeOf(_cxt, n, cw, ch, cle.replace("flas-sizing-", ""));
        break;
      }
    }
  }
};
FLCard.prototype._setSizeOf = function(_cxt, img, cw, ch, alg) {
  var parent = img.parentElement;
  if (alg.startsWith("target-center-")) {
    parent.style.position = "relative";
    var props = alg.replace("target-center-", "");
    var idx = props.indexOf("-");
    var xp = parseFloat(props.substring(0, idx));
    var yp = parseFloat(props.substring(idx + 1));
    var vprat = cw / ch;
    var imgrat = img.width / img.height;
    if (isNaN(imgrat))
      return;
    if (vprat < imgrat) {
      parent.style.height = ch;
      img.style.height = ch;
      parent.style.width = "auto";
      img.style.width = "auto";
      var newImgWid = ch * imgrat;
      var left = -(newImgWid * xp / 100 - cw / 2);
      if (left + newImgWid < cw) {
        left = cw - newImgWid;
        if (left > 0)
          left /= 2;
      }
      parent.style.left = left;
    } else {
      parent.style.width = cw;
      img.style.width = cw;
      parent.style.height = "auto";
      img.style.height = "auto";
      parent.style.left = 0;
      var newImgHt = cw / imgrat;
      var top = -(newImgHt * yp / 100 - ch / 2);
      if (top + newImgHt < ch) {
        top = ch - newImgHt;
        if (top > 0)
          top /= 2;
      }
      parent.style.top = top;
    }
  } else if (alg.startsWith("min-aspect-")) {
    parent.style.position = "relative";
    var props = alg.replace("min-aspect-", "");
    var idx = props.indexOf("-");
    var idx2 = props.indexOf("-", idx + 1);
    var idx3 = props.indexOf("-", idx2 + 1);
    var idx4 = props.indexOf("-", idx3 + 1);
    var xc = parseFloat(props.substring(0, idx)) * cw / 100;
    var yc = parseFloat(props.substring(idx + 1, idx2)) * ch / 100;
    var pct = parseFloat(props.substring(idx2 + 1, idx3)) / 100;
    var xr = parseFloat(props.substring(idx3 + 1, idx4));
    var yr = parseFloat(props.substring(idx4 + 1));
    var xp = cw * pct;
    var yp = ch * pct;
    var mp = Math.min(xp, yp);
    xr = xr * mp;
    yr = yr * mp;
    img.style.width = xr;
    img.style.height = yr;
    img.style.left = xc - xr / 2;
    img.style.top = yc - yr / 2;
  } else if (alg.startsWith("promote-box-")) {
    parent.style.position = "relative";
    var props = alg.replace("promote-box-", "");
    var idx = props.indexOf("-");
    var idx2 = props.indexOf("-", idx + 1);
    var ar = parseFloat(props.substring(0, idx));
    var sm, rotate = false;
    if (idx2 != -1) {
      sm = parseFloat(props.substring(idx + 1, idx2)) / 100;
      rotate = props.substring(idx2 + 1) == "rotate";
    } else {
      sm = parseFloat(props.substring(idx + 1)) / 100;
    }
    var md = Math.min(cw / ar, ch);
    var dw = md * ar * (1 - 2 * sm), dh = md * (1 - 2 * sm);
    img.style.width = dw;
    img.style.height = dh;
    if (sm > 0) {
      img.style.borderTopWidth = img.style.borderBottomWidth = md * sm;
      img.style.borderLeftWidth = img.style.borderRightWidth = md * ar * sm;
    }
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
};
FLCard.prototype._currentDiv = function(cx2) {
  if (this._renderTree)
    return document.getElementById(this._renderTree._id);
  else
    return this._containedIn;
};
FLCard.prototype._currentRenderTree = function() {
  return this._renderTree;
};
FLCard.prototype._attachHandlers = function(_cxt, rt, div, key, field, option, source, evconds) {
  const evcs = this._eventHandlers()[key];
  if (evcs) {
    if (rt && rt.handlers) {
      for (var i = 0; i < rt.handlers.length; i++) {
        var rh = rt.handlers[i];
        div.removeEventListener(rh.hi.event._eventName, rh.eh);
      }
      delete rt.handlers;
    }
    for (var ej = 0; ej < evcs.length; ej++) {
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
      if (evconds && typeof handlerInfo.cond !== "undefined") {
        if (!evconds[handlerInfo.cond])
          continue;
      }
      var eh = _cxt.attachEventToCard(this, handlerInfo, div, { value: source });
      if (eh && rt) {
        if (!rt.handlers) {
          rt.handlers = [];
        }
        rt.handlers.push({ hi: handlerInfo, eh });
      }
    }
  }
};
FLCard.prototype._updateContent = function(_cxt, rt, templateName, field, option, source, value, fromField) {
  if (!rt)
    return;
  value = _cxt.full(value);
  if (typeof value === "undefined" || value == null)
    value = "";
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
  if (value instanceof Html) {
    node.innerHTML = value.state.get("html");
  } else {
    node.innerHTML = "";
    node.appendChild(document.createTextNode(value));
  }
  if (this._eventHandlers) {
    this._attachHandlers(_cxt, rt[field], node, templateName, field, option, source);
  }
};
FLCard.prototype._updateImage = function(_cxt, rt, templateName, field, option, source, value, fromField) {
  if (!rt)
    return;
  value = _cxt.full(value);
  if (typeof value === "undefined" || value == null || !(value instanceof Image))
    value = "";
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
  node.onload = function(ev) {
    self._imageLoaded(_cxt);
  };
};
FLCard.prototype._updateLink = function(_cxt, rt, templateName, field, option, source, value, fromField) {
  if (!rt)
    return;
  value = _cxt.full(value);
  var linkRef;
  var linkText;
  if (typeof value === "undefined" || value == null || !(value instanceof Link))
    linkRef = linkText = "";
  else {
    linkRef = value._field_uri(_cxt).uri;
    linkTitle = value._field_title(_cxt);
  }
  var div = document.getElementById(rt._id);
  const node = div.querySelector("[data-flas-link='" + field + "']");
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
  var env2 = _cxt.env;
  node.onclick = (ev) => window.appl.gotoRoute(env2.newContext(), linkRef);
  node.dataset.route = linkRef;
  node.innerText = linkTitle;
};
FLCard.prototype._imageLoaded = function(_cxt) {
  this._resizeDisplayElements(_cxt, this._renderTree);
};
FLCard.prototype._updateFromInputs = function() {
  if (this._renderTree)
    this._updateFromEachInput(this._renderTree);
};
FLCard.prototype._updateFromEachInput = function(rt) {
  if (rt.children) {
    for (var i = 0; i < rt.children.length; i++) {
      this._updateFromEachInput(rt.children[i]);
    }
  }
  var props = Object.keys(rt);
  for (var i = 0; i < props.length; i++) {
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
};
FLCard.prototype._updateStyle = function(_cxt, rt, templateName, type, field, option, source, constant, ...rest) {
  if (!rt)
    return;
  var styles = "";
  if (constant)
    styles = _cxt.full(constant);
  var evconds = [];
  for (var i = 0; i < rest.length; i += 2) {
    if (_cxt.isTruthy(rest[i])) {
      styles += " " + _cxt.full(rest[i + 1]);
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
};
FLCard.prototype._updateTemplate = function(_cxt, _renderTree, type, field, fn, templateName, value, _tc) {
  if (!_renderTree)
    return;
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
    node.innerHTML = "";
    if (!value)
      return;
    var t = document.getElementById(templateName);
    if (t != null) {
      if (Array.isArray(value)) {
        if (!crt.children) {
          crt.children = [];
        }
        var card = this;
        this._updateList(_cxt, node, crt.children, value, {
          insert: function(rtc, ni, v) {
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
        if (crt.single) {
          this._addItem(_cxt, crt.single, node, node.firstElementChild, t, fn, value, _tc);
        } else {
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
};
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
};
FLCard.prototype._updateContainer = function(_cxt, _renderTree, field, value, fn) {
  if (!_renderTree)
    return;
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
    node.innerHTML = "";
    crt.children = [];
    return;
  }
  var card = this;
  if (Array.isArray(value)) {
    this._updateList(_cxt, node, crt.children, value, {
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
    var curr = null;
    if (!crt.single)
      crt.single = {};
    else if (value == crt.single.value) {
      curr = node.firstElementChild;
    } else {
      node.innerHTML = "";
      crt.single = {};
    }
    fn.call(card, _cxt, crt.single, node, curr, value);
  }
};
FLCard.prototype._updatePunnet = function(_cxt, _renderTree, field, value, fn) {
  if (!_renderTree)
    return;
  value = _cxt.full(value);
  var div = document.getElementById(_renderTree._id);
  const node = div.querySelector("[data-flas-punnet='" + field + "']");
  if (!node.id) {
    var ncid = _cxt.nextDocumentId();
    node.id = ncid;
    _renderTree[field] = { _id: ncid, children: [] };
  }
  var crt = _renderTree[field];
  if (value instanceof FLError2) {
    _cxt.log("error cannot be rendered", value);
    value = null;
  }
  if (!value) {
    node.innerHTML = "";
    crt.children = [];
    return;
  } else if (value instanceof FLCard) {
    if (crt.children.length == 1 && crt.children[0].value == value)
      return;
    for (var i = 0; i < crt.children.length; i++) {
      crt.children[i].value._renderTree = null;
    }
    crt.children = [];
    node.innerHTML = "";
    var inid = _cxt.nextDocumentId();
    crt.children.push({ value });
    const pe = document.createElement("div");
    pe.setAttribute("id", inid);
    node.appendChild(pe);
    value._renderInto(_cxt, pe);
  } else if (Array.isArray(value)) {
    var sw = this._diffLists(_cxt, crt.children, value);
    if (sw === true) {
      for (var i = 0; i < value.length; i++) {
        value[i]._updateDisplay(_cxt, value[i]._renderTree);
      }
    } else if (sw.op === "addtoend") {
      for (var i = crt.children.length; i < value.length; i++) {
        if (value[i] instanceof FLCard) {
          var inid = _cxt.nextDocumentId();
          crt.children.push({ value: value[i] });
          const pe = document.createElement("div");
          pe.setAttribute("id", inid);
          node.appendChild(pe);
          value[i]._renderInto(_cxt, pe);
        } else {
          throw new Error("not a card: " + value);
        }
      }
    } else if (sw.op === "add") {
      for (var i = 0; i < sw.additions.length; i++) {
        var ai = sw.additions[i];
        var e = ai.value;
        var rt = { value: e };
        crt.children.splice(ai.where, 0, rt);
        if (e instanceof FLCard) {
          var inid = _cxt.nextDocumentId();
          const pe = document.createElement("div");
          pe.setAttribute("id", inid);
          node.appendChild(pe);
          e._renderInto(_cxt, pe);
        } else {
          throw new Error("not a card: " + value);
        }
        if (ai.where < node.childElementCount - 1)
          node.insertBefore(node.lastElementChild, node.children[ai.where]);
      }
    } else {
      throw new Error("cannot handle punnet change: " + sw.op);
    }
  } else
    throw new Error("what is this? " + value);
};
FLCard.prototype._updateList = function(cx2, parent, rts, values, cb) {
  var sw = this._diffLists(cx2, rts, values);
  if (sw === true) {
    for (var i = 0; i < values.length; i++) {
      cb.insert(rts[i], parent.children[i], values[i]);
    }
  } else if (sw.op === "addtoend") {
    for (var i = 0; i < rts.length; i++) {
      cb.insert(rts[i], parent.children[i], values[i]);
    }
    for (var i = rts.length; i < values.length; i++) {
      var e = values[i];
      var rt = { value: e };
      rts.push(rt);
      cb.insert(rt, null, e);
    }
  } else if (sw.op === "add") {
    var done = [];
    for (var i = 0; i < sw.additions.length; i++) {
      var ai = sw.additions[i];
      var e = ai.value;
      var rt = { value: e };
      rts.splice(ai.where, 0, rt);
      cb.insert(rt, null, e);
      if (ai.where < parent.childElementCount - 1)
        parent.insertBefore(parent.lastElementChild, parent.children[ai.where]);
      done.push(ai.where);
    }
    for (var i = 0; i < values.length; i++) {
      if (!done.includes(i))
        cb.insert(rts[i], parent.children[i], values[i]);
    }
  } else if (sw.op === "removefromend") {
    rts.splice(values.length);
    while (values.length < parent.childElementCount) {
      parent.lastChild.remove();
    }
    for (var i = 0; i < values.length; i++) {
      cb.insert(rts[i], parent.children[i], values[i]);
    }
  } else if (sw.op === "remove") {
    for (var i = 0; i < sw.removals.length; i++) {
      var ri = sw.removals[i];
      rts.splice(ri.where, 1);
      parent.children[ri.where].remove();
    }
    for (var i = 0; i < values.length; i++) {
      cb.insert(rts[i], parent.children[i], values[i]);
    }
  } else if (sw.op === "disaster") {
    var map = {};
    while (parent.firstElementChild) {
      var nd = parent.removeChild(parent.firstElementChild);
      var rtc = rts.shift();
      map[nd.id] = { nd, rtc };
    }
    console.log("disaster map", sw.mapping, map);
    for (var i = 0; i < values.length; i++) {
      if (sw.mapping[i]) {
        var tmp = map[sw.mapping[i]];
        parent.appendChild(tmp.nd);
        rts.push(tmp.rtc);
        delete map[sw.mapping[i]];
      } else {
        var e = values[i];
        var rt = { value: e };
        rts.push(rt);
        cb.insert(rt, null, e);
      }
    }
  } else {
    throw new Error("not handled: " + sw.op);
  }
};
FLCard.prototype._updateCrobag = function(parent, rts, crobag, callback) {
  var scrollInfo = this._figureScrollInfo(parent);
  for (var i = 0; i < crobag.size(); i++) {
    var e = crobag._entries[i];
    if (i >= rts.length) {
      var rt = { value: e };
      rts.push(rt);
      callback.insert(rt, null, e.val);
    } else if (e.key == rts[i].value.key) {
      callback.insert(rts[i], parent.children[i], e.val);
    } else if (e.key < rts[i].value.key) {
      var rt = { value: e };
      rts.splice(i, 0, rt);
      callback.insert(rt, null, e.val);
      parent.insertBefore(parent.lastElementChild, parent.children[i]);
    } else if (e.key > rts[i].value.key) {
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
};
FLCard.prototype._figureScrollInfo = function(parent) {
  var div = parent;
  while (div != document.body) {
    var oy = window.getComputedStyle(div)["overflowY"];
    if (oy == "scroll" || oy == "auto")
      break;
    div = div.parentElement;
  }
  var min = div.scrollTop;
  var max = min + div.clientHeight;
  var mid = (min + max) / 2;
  var ret2 = { lockMode: "bottom", lockOffset: 0, scroller: div, ht: 0, scrollht: div.scrollHeight, scrollTop: div.scrollTop, viewport: div.clientHeight };
  var nodes = parent.children;
  if (nodes.length == 0) {
    return ret2;
  }
  var top = nodes[0];
  var bottom = nodes[nodes.length - 1];
  if (bottom.offsetTop < max) {
    ret2.lockMode = "bottom";
    ret2.lockOffset = ret2.scrollht - ret2.scrollTop;
  } else if (top.offsetTop + top.offsetHeight > min) {
    ret2.lockMode = "top";
    ret2.lockOffset = ret2.scrollTop - top.offsetTop;
  } else {
    for (var i = 0; i < nodes.length; i++) {
      if (nodes[i].offsetTop + nodes[i].offsetHeight >= mid) {
        ret2.lockMode = "mid";
        ret2.lockDiv = nodes[i];
        ret2.lockOffset = nodes[i].offsetTop - ret2.scrollTop;
        break;
      }
    }
  }
  return ret2;
};
FLCard.prototype._diffLists = function(_cxt, rtc, list) {
  var ret2 = { additions: [], removals: [], mapping: {} };
  var added = false, removed = false;
  var used = {};
  outer:
    for (var i = 0, j = 0; i < rtc.length && j < list.length; j++) {
      if (_cxt.compare(rtc[i].value, list[j])) {
        ret2.mapping[j] = rtc[i]._id;
        used[i] = true;
        i++;
      } else {
        for (var k = i + 1; k < rtc.length; k++) {
          if (list[j] === rtc[k].value) {
            ret2.mapping[j] = rtc[k]._id;
            used[k] = true;
            ret2.removals.unshift({ where: i });
            i = k + 1;
            removed = true;
            continue outer;
          }
        }
        for (var k = j + 1; k < list.length; k++) {
          if (list[k] === rtc[i].value) {
            ret2.mapping[k] = rtc[i]._id;
            ret2.additions.unshift({ where: i, value: list[j] });
            added = true;
            continue outer;
          }
        }
        for (var k = i - 1; k >= 0; k--) {
          if (used[k])
            continue;
          if (list[j] == rtc[k].value) {
            ret2.mapping[j] = rtc[k]._id;
            used[k] = true;
            break;
          }
        }
        added = removed = true;
        i++;
      }
    }
  if ((added || j < list.length) && (removed || i < rtc.length)) {
    ret2.op = "disaster";
    while (j < list.length) {
      for (var k = 0; k < rtc.length; k++) {
        if (used[k])
          continue;
        if (rtc[k].value == list[j]) {
          ret2.mapping[j] = rtc[k]._id;
          used[k] = true;
          break;
        }
      }
      j++;
    }
  } else if (added) {
    ret2.op = "add";
    while (j < list.length) {
      ret2.additions.unshift({ where: i++, value: list[j++] });
    }
  } else if (removed) {
    ret2.op = "remove";
    while (i < rtc.length) {
      ret2.removals.unshift({ where: i++ });
    }
  } else if (list.length > rtc.length) {
    ret2.op = "addtoend";
  } else if (list.length < rtc.length) {
    ret2.op = "removefromend";
  } else
    return true;
  return ret2;
};
FLCard.prototype._close = function(cx2) {
  cx2.unsubscribeAll(this);
};

// src/main/javascript/runtime/object.js
var FLObject2 = function(cx2) {
};
FLObject2.prototype._updateTemplate = FLCard.prototype._updateTemplate;
FLObject2.prototype._addItem = FLCard.prototype._addItem;
FLObject2.prototype._updateContent = FLCard.prototype._updateContent;
FLObject2.prototype._updateContainer = FLCard.prototype._updateContainer;
FLObject2.prototype._updatePunnet = FLCard.prototype._updatePunnet;
FLObject2.prototype._updateStyle = FLCard.prototype._updateStyle;
FLObject2.prototype._updateList = FLCard.prototype._updateList;
FLObject2.prototype._updateImage = FLCard.prototype._updateImage;
FLObject2.prototype._updateLink = FLCard.prototype._updateLink;
FLObject2.prototype._diffLists = FLCard.prototype._diffLists;
FLObject2.prototype._attachHandlers = FLCard.prototype._attachHandlers;
FLObject2.prototype._resizeDisplayElements = FLCard.prototype._resizeDisplayElements;
var object_default = FLObject2;

// src/main/javascript/runtime/random.js
function xoshiro128(a, b, c, d) {
  function rotl(x, k) {
    return x << k | x >> 32 - k;
  }
  return function() {
    var result = rotl(a + d, 7) + a;
    var t = b << 9;
    c ^= a;
    d ^= b;
    b ^= c;
    a ^= d;
    c ^= t;
    d = rotl(d, 11);
    return result & 2147483647;
  };
}
var Random = function(_cxt, _card) {
  object_default.call(this, _cxt);
  this._card = _card;
  this.state = _cxt.fields();
  this.buffer = [];
};
Random._ctor_seed = function(_cxt, _card, s) {
  const ret2 = new Random(_cxt, _card);
  var seed = s ^ 3735928559;
  ret2.generateNext = xoshiro128(2654435769, 608135816, 3084996962, seed);
  return new ResponseWithMessages2(_cxt, ret2, []);
};
Random._ctor_seed.nfargs = function() {
  return 2;
};
Random._ctor_unseeded = function(_cxt, _card) {
  const ret2 = new Random(_cxt, _card);
  var seed = Math.random() * 4294967295;
  ret2.generateNext = xoshiro128(2654435769, 608135816, 3084996962, seed);
  return new ResponseWithMessages2(_cxt, ret2, []);
};
Random._ctor_unseeded.nfargs = function() {
  return 1;
};
Random.prototype.next = function(_cxt, quant) {
  while (this.buffer.length < quant)
    this.buffer.push(this.generateNext());
  return this.buffer.slice(0, quant);
};
Random.prototype.next.nfargs = function() {
  return 1;
};
Random.prototype.used = function(_cxt, quant) {
  return Send2.eval(_cxt, this, "_used", [quant]);
};
Random.prototype.used.nfargs = function() {
  return 1;
};
Random.prototype._used = function(_cxt, quant) {
  while (quant-- > 0 && this.buffer.length > 0)
    this.buffer.shift();
};
Random.prototype._used.nfargs = function() {
  return 1;
};
Random.prototype._methods = function() {
  return {
    "used": Random.prototype.used,
    "_used": Random.prototype._used
  };
};

// src/main/javascript/runtime/crobag.js
var SlideWindow = function(_cxt) {
  IdempotentHandler.call(this, _cxt);
  return;
};
SlideWindow.prototype = new IdempotentHandler();
SlideWindow.prototype.constructor = SlideWindow;
SlideWindow.prototype.name = function() {
  return "SlideWindow";
};
SlideWindow.prototype.name.nfargs = function() {
  return -1;
};
SlideWindow.prototype._methods = function() {
  const v1 = ["success", "failure"];
  return v1;
};
SlideWindow.prototype._methods.nfargs = function() {
  return -1;
};
var CrobagWindow = function(_cxt) {
  IdempotentHandler.call(this, _cxt);
  return;
};
CrobagWindow.prototype = new IdempotentHandler();
CrobagWindow.prototype.constructor = CrobagWindow;
CrobagWindow.prototype.name = function() {
  return "CrobagWindow";
};
CrobagWindow.prototype.name.nfargs = function() {
  return -1;
};
CrobagWindow.prototype._methods = function() {
  const v1 = ["success", "failure", "next", "done"];
  return v1;
};
CrobagWindow.prototype._methods.nfargs = function() {
  return -1;
};
CrobagWindow.prototype.next = function(_cxt, _key, _value, _ih) {
  return "interface method for CrobagWindow.next";
};
CrobagWindow.prototype.next.nfargs = function() {
  return 2;
};
CrobagWindow.prototype.done = function(_cxt, _ih) {
  return "interface method for CrobagWindow.done";
};
CrobagWindow.prototype.done.nfargs = function() {
  return 0;
};
var CroEntry = function(key, val) {
  this.key = key;
  this.val = val;
};
CroEntry.fromWire = function(cx2, om2, fields2) {
  var lt = new ListTraverser(cx2, om2.state);
  om2.marshal(lt, fields2["value"]);
  return new CroEntry(fields2["key"], lt.ret[0]);
};
var Crobag2 = function(_cxt, _card) {
  object_default.call(this, _cxt);
  this._card = _card;
  this.state = { dict: {} };
  this._entries = [];
};
Crobag2._ctor_new = function(_cxt, _card) {
  const ret2 = new Crobag2(_cxt, _card);
  return new ResponseWithMessages2(_cxt, ret2, []);
};
Crobag2._ctor_new.nfargs = function() {
  return 1;
};
Crobag2.fromWire = function(cx2, om2, fields2) {
  var ret2 = new Crobag2(cx2, null);
  var os = fields2["entries"];
  if (os.length > 0) {
    var lt = new ListTraverser(cx2, om2.state);
    for (var i = 0; i < os.length; i++) {
      om2.marshal(lt, os[i]);
    }
    ret2._entries = lt.ret;
  }
  return ret2;
};
Crobag2.prototype._towire = function(wf) {
  wf._wireable = "org.flasck.jvm.builtin.Crobag";
  var os = fields["entries"];
  if (os.length > 0) {
    var lt = new ListTraverser(cx, om.state);
    for (var i = 0; i < os.length; i++) {
      om.marshal(lt, os[i]);
    }
    ret._entries = lt.ret;
  }
  return ret;
};
Crobag2.prototype.insert = function(_cxt, key, val) {
  return [CrobagChangeEvent.eval(_cxt, this, "insert", key, null, val)];
};
Crobag2.prototype.insert.nfargs = function() {
  return 1;
};
Crobag2.prototype.put = function(_cxt, key, val) {
  return [CrobagChangeEvent.eval(_cxt, this, "put", key, null, val)];
};
Crobag2.prototype.put.nfargs = function() {
  return 1;
};
Crobag2.prototype.upsert = function(_cxt, key, val) {
  return [CrobagChangeEvent.eval(_cxt, this, "upsert", key, null, val)];
};
Crobag2.prototype.upsert.nfargs = function() {
  return 1;
};
Crobag2.prototype.window = function(_cxt, from, size, handler) {
  return [CrobagWindowEvent.eval(_cxt, this, from, size, handler)];
};
Crobag2.prototype.window.nfargs = function() {
  return 3;
};
Crobag2.prototype.size = function(_cxt) {
  return this._entries.length;
};
Crobag2.prototype.size.nfargs = function() {
  return 0;
};
Crobag2.prototype._change = function(cx2, op, newKey, remove, val) {
  if (newKey != null) {
    var e = new CroEntry(newKey, val);
    var done = false;
    for (var i = 0; i < this._entries.length; i++) {
      if (this._entries[i].key > newKey) {
        this._entries.splice(i, 0, e);
        done = true;
        break;
      } else if (this._entries[i].key == newKey) {
        if (op == "insert") {
          continue;
        } else if (op == "put") {
          this._entries.splice(i, 1, e);
        } else if (op == "upsert") {
        }
        done = true;
        break;
      }
    }
    if (!done)
      this._entries.push(e);
  }
};
Crobag2.prototype._methods = function() {
  return {
    "insert": Crobag2.prototype.insert,
    "put": Crobag2.prototype.put,
    "size": Crobag2.prototype.size,
    "upsert": Crobag2.prototype.upsert,
    "window": Crobag2.prototype.window
  };
};
var CrobagChangeEvent = function() {
};
CrobagChangeEvent.eval = function(_cxt, bag, op, newKey, remove, val) {
  const e = new CrobagChangeEvent();
  e.bag = bag;
  e.op = op;
  e.newKey = newKey;
  e.remove = remove;
  e.val = val;
  return e;
};
CrobagChangeEvent.prototype._compare = function(cx2, other) {
  if (other instanceof CrobagChangeEvent) {
    return other.msg == this.msg;
  } else
    return false;
};
CrobagChangeEvent.prototype.dispatch = function(cx2) {
  this.bag = cx2.full(this.bag);
  if (this.bag instanceof FLError)
    return this.bag;
  this.op = cx2.full(this.op);
  if (this.op instanceof FLError)
    return this.op;
  this.newKey = cx2.full(this.newKey);
  if (this.newKey instanceof FLError)
    return this.newKey;
  this.remove = cx2.full(this.remove);
  if (this.remove instanceof FLError)
    return this.remove;
  this.val = cx2.full(this.val);
  if (this.val instanceof FLError)
    return this.val;
  this.bag._change(cx2, this.op, this.newKey, this.remove, this.val);
  return [];
};
CrobagChangeEvent.prototype.toString = function() {
  return "CrobagChangeEvent[" + this.from + ":" + this.size + "]";
};
var CrobagWindowEvent = function() {
};
CrobagWindowEvent.eval = function(_cxt, bag, from, size, replyto) {
  const e = new CrobagWindowEvent();
  e.bag = bag;
  e.from = from;
  e.size = size;
  e.replyto = replyto;
  return e;
};
CrobagWindowEvent.prototype._compare = function(cx2, other) {
  if (other instanceof CrobagWindowEvent) {
    return other.msg == this.msg;
  } else
    return false;
};
CrobagWindowEvent.prototype.dispatch = function(cx2) {
  this.bag = cx2.full(this.bag);
  if (this.bag instanceof FLError)
    return this.bag;
  this.from = cx2.full(this.from);
  if (this.from instanceof FLError)
    return this.from;
  this.size = cx2.full(this.size);
  if (this.size instanceof FLError)
    return this.size;
  this.replyto = cx2.full(this.replyto);
  if (this.replyto instanceof FLError)
    return this.replyto;
  var arr = [];
  var k = 0;
  for (var i = 0; i < this.bag._entries.length; i++) {
    var e = this.bag._entries[i];
    if (e.key < this.from)
      continue;
    if (k >= this.size)
      break;
    arr.push(Send2.eval(cx2, this.replyto, "next", [e.key, e.val], null));
  }
  arr.push(Send2.eval(cx2, this.replyto, "done", [], _ActualSlideHandler.eval(cx2, this.crobag)));
  return arr;
};
CrobagWindowEvent.prototype.toString = function() {
  return "CrobagWindowEvent[" + this.from + ":" + this.size + "]";
};
var _ActualSlideHandler = function(_cxt, crobag) {
  SlideWindow.call(this, _cxt);
  this.state = _cxt.fields();
  this._card = crobag;
  return;
};
_ActualSlideHandler.prototype = new SlideWindow();
_ActualSlideHandler.prototype.constructor = _ActualSlideHandler;
_ActualSlideHandler.eval = function(_cxt, crobag) {
  const v1 = new _ActualSlideHandler(_cxt, crobag);
  v1.state.set("_type", "_ActualSlideHandler");
  return v1;
};
_ActualSlideHandler.eval.nfargs = function() {
  return 1;
};
_ActualSlideHandler.prototype._card = function() {
  return this._card;
};
_ActualSlideHandler.prototype._card.nfargs = function() {
  return -1;
};

// src/main/javascript/runtime/image.js
var Image2 = function(_cxt, _uri) {
  object_default.call(this, _cxt);
  this.state = _cxt.fields();
  this.state.set("uri", _uri);
};
Image2._ctor_asset = function(_cxt, _card, _uri) {
  const ret2 = new Image2(_cxt, _uri);
  return new ResponseWithMessages2(_cxt, ret2, []);
};
Image2._ctor_asset.nfargs = function() {
  return 2;
};
Image2._ctor_uri = function(_cxt, _card, _uri) {
  const ret2 = new Image2(_cxt, _uri);
  return new ResponseWithMessages2(_cxt, ret2, []);
};
Image2._ctor_uri.nfargs = function() {
  return 2;
};
Image2.prototype.getUri = function() {
  var uri = this.state.get("uri");
  if (uri instanceof FLURI)
    uri = uri.resolve(window.location);
  return uri;
};
Image2.prototype._compare = function(_cxt, other) {
  if (!(other instanceof Image2))
    return false;
  return this.state.get("uri").toString() == other.state.get("uri").toString();
};
Image2.prototype.toString = function() {
  return "Image " + this.state.get("uri");
};

// src/main/javascript/runtime/link.js
var Link2 = function(_cxt) {
  this.state = _cxt.fields();
  this.state.set("_type", "Link");
};
Link2._typename = "Link";
Link2.prototype._areYouA = function(_cxt, ty) {
  if (_cxt.isTruthy(ty == "Link")) {
    return true;
  } else
    return false;
};
Link2.prototype._areYouA.nfargs = function() {
  return 1;
};
Link2.eval = function(_cxt, _uri, _title) {
  var v1 = new Link2(_cxt);
  v1.state.set("uri", _uri);
  v1.state.set("title", _title);
  return v1;
};
Link2.eval.nfargs = function() {
  return 2;
};
Link2.prototype._field_title = function(_cxt) {
  return this.state.get("title");
};
Link2.prototype._field_title.nfargs = function() {
  return 0;
};
Link2.prototype._field_uri = function(_cxt) {
  return this.state.get("uri");
};
Link2.prototype._field_uri.nfargs = function() {
  return 0;
};

// src/main/javascript/runtime/date.format.js
var dateFormat = /* @__PURE__ */ function() {
  var token = /d{1,4}|m{1,4}|yy(?:yy)?|([HhMsTt])\1?|[LloSZ]|"[^"]*"|'[^']*'/g, timezone = /\b(?:[PMCEA][SDP]T|(?:Pacific|Mountain|Central|Eastern|Atlantic) (?:Standard|Daylight|Prevailing) Time|(?:GMT|UTC)(?:[-+]\d{4})?)\b/g, timezoneClip = /[^-+\dA-Z]/g, pad = function(val, len) {
    val = String(val);
    len = len || 2;
    while (val.length < len)
      val = "0" + val;
    return val;
  };
  return function(date, mask, utc) {
    var dF = dateFormat;
    if (arguments.length == 1 && Object.prototype.toString.call(date) == "[object String]" && !/\d/.test(date)) {
      mask = date;
      date = void 0;
    }
    date = date ? new Date(date) : /* @__PURE__ */ new Date();
    if (isNaN(date))
      throw SyntaxError("invalid date");
    mask = String(dF.masks[mask] || mask || dF.masks["default"]);
    if (mask.slice(0, 4) == "UTC:") {
      mask = mask.slice(4);
      utc = true;
    }
    var _ = utc ? "getUTC" : "get", d = date[_ + "Date"](), D = date[_ + "Day"](), m = date[_ + "Month"](), y = date[_ + "FullYear"](), H = date[_ + "Hours"](), M = date[_ + "Minutes"](), s = date[_ + "Seconds"](), L = date[_ + "Milliseconds"](), o = utc ? 0 : date.getTimezoneOffset(), flags = {
      d,
      dd: pad(d),
      ddd: dF.i18n.dayNames[D],
      dddd: dF.i18n.dayNames[D + 7],
      m: m + 1,
      mm: pad(m + 1),
      mmm: dF.i18n.monthNames[m],
      mmmm: dF.i18n.monthNames[m + 12],
      yy: String(y).slice(2),
      yyyy: y,
      h: H % 12 || 12,
      hh: pad(H % 12 || 12),
      H,
      HH: pad(H),
      M,
      MM: pad(M),
      s,
      ss: pad(s),
      l: pad(L, 3),
      L: pad(L > 99 ? Math.round(L / 10) : L),
      t: H < 12 ? "a" : "p",
      tt: H < 12 ? "am" : "pm",
      T: H < 12 ? "A" : "P",
      TT: H < 12 ? "AM" : "PM",
      Z: utc ? "UTC" : (String(date).match(timezone) || [""]).pop().replace(timezoneClip, ""),
      o: (o > 0 ? "-" : "+") + pad(Math.floor(Math.abs(o) / 60) * 100 + Math.abs(o) % 60, 4),
      S: ["th", "st", "nd", "rd"][d % 10 > 3 ? 0 : (d % 100 - d % 10 != 10) * d % 10]
    };
    return mask.replace(token, function($0) {
      return $0 in flags ? flags[$0] : $0.slice(1, $0.length - 1);
    });
  };
}();
dateFormat.masks = {
  "default": "ddd mmm dd yyyy HH:MM:ss",
  shortDate: "m/d/yy",
  mediumDate: "mmm d, yyyy",
  longDate: "mmmm d, yyyy",
  fullDate: "dddd, mmmm d, yyyy",
  shortTime: "h:MM TT",
  mediumTime: "h:MM:ss TT",
  longTime: "h:MM:ss TT Z",
  isoDate: "yyyy-mm-dd",
  isoTime: "HH:MM:ss",
  isoDateTime: "yyyy-mm-dd'T'HH:MM:ss",
  isoUtcDateTime: "UTC:yyyy-mm-dd'T'HH:MM:ss'Z'"
};
dateFormat.i18n = {
  dayNames: [
    "Sun",
    "Mon",
    "Tue",
    "Wed",
    "Thu",
    "Fri",
    "Sat",
    "Sunday",
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday"
  ],
  monthNames: [
    "Jan",
    "Feb",
    "Mar",
    "Apr",
    "May",
    "Jun",
    "Jul",
    "Aug",
    "Sep",
    "Oct",
    "Nov",
    "Dec",
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December"
  ]
};

// src/main/javascript/runtime/time.js
var Interval = function(d, ns2) {
  this.days = d;
  this.ns = ns2;
};
Interval.prototype.asJs = function() {
  return this.days * 864e5 + this.ns / 1e3 / 1e3;
};
Interval.prototype._towire = function(wf) {
  wf.days = days;
  wf.ns = ns;
};
var Instant = function(d, ns2) {
  this.days = d;
  this.ns = ns2;
};
Instant.prototype.asJs = function() {
  return this.days * 864e5 + this.ns / 1e3 / 1e3;
};
Instant.prototype._towire = function(wf) {
  wf.days = days;
  wf.ns = ns;
};
FLBuiltin.seconds = function(_cxt, n) {
  n = _cxt.full(n);
  if (n instanceof FLError2)
    return n;
  else if (typeof n !== "number")
    return new FLError2("not a number");
  return new Interval(Math.floor(n / 86400), n % 86400 * 1e3 * 1e3 * 1e3);
};
FLBuiltin.seconds.nfargs = function() {
  return 1;
};
FLBuiltin.milliseconds = function(_cxt, n) {
  n = _cxt.full(n);
  if (n instanceof FLError2)
    return n;
  else if (typeof n !== "number")
    return new FLError2("not a number");
  return new Interval(Math.floor(n / 864e5), n % 864e5 * 1e3 * 1e3 * 1e3);
};
FLBuiltin.milliseconds.nfargs = function() {
  return 1;
};
FLBuiltin.fromunixdate = function(_cxt, n) {
  n = _cxt.full(n);
  if (n instanceof FLError2)
    return n;
  else if (typeof n !== "number")
    return new FLError2("not a number");
  return new Instant(Math.floor(n / 86400), n % 86400 * 1e3 * 1e3 * 1e3);
};
FLBuiltin.fromunixdate.nfargs = function() {
  return 1;
};
FLBuiltin.unixdate = function(_cxt, i) {
  i = _cxt.full(i);
  if (i instanceof FLError2)
    return i;
  else if (!(i instanceof Instant))
    return new FLError2("not an instant");
  var ds = i.days;
  var secs = i.ns / 1e3 / 1e3 / 1e3;
  return ds * 86400 + secs;
};
FLBuiltin.unixdate.nfargs = function() {
  return 1;
};
var Calendar = function(_cxt, _card) {
  object_default.call(this, _cxt);
  this._card = _card;
  this.state = _cxt.fields();
};
Calendar._ctor_gregorian = function(_cxt, _card) {
  const ret2 = new Calendar(_cxt, _card);
  return new ResponseWithMessages2(_cxt, ret2, []);
};
Calendar._ctor_gregorian.nfargs = function() {
  return 1;
};
Calendar.prototype.isoDateTime = function(_cxt, inst) {
  inst = _cxt.full(inst);
  if (inst instanceof FLError2)
    return inst;
  else if (!(inst instanceof Instant))
    return new FLError2("not an instant");
  return dateFormat(new Date(inst.asJs()), dateFormat.masks.isoUtcDateTime);
};
Calendar.prototype.isoDateTime.nfargs = function() {
  return 1;
};
Calendar.prototype._parseIsoItem = function(cursor, nd, decimal) {
  if (typeof nd == "undefined") {
    nd = 2;
  }
  if (cursor.pos >= cursor.str.length)
    return 0;
  while (cursor.str[cursor.pos] == ":" || cursor.str[cursor.pos] == "-")
    ;
  if (cursor.pos >= cursor.str.length)
    return 0;
  var ret2 = 0;
  for (var i = 0; i < nd && cursor.pos < cursor.str.length; i++) {
    var c = cursor.str[cursor.pos++];
    if (c < "0" || c > "9")
      break;
    if (decimal) {
      ret2 = ret2 + (c - "0") * decimal;
      decimal /= 10;
    } else {
      ret2 = ret2 * 10 + (c - "0");
    }
  }
  return ret2;
};
Calendar.prototype.parseIsoDateTime = function(_cxt, n) {
  n = _cxt.full(n);
  if (n instanceof FLError2)
    return n;
  else if (typeof n !== "string")
    return new FLError2("not a string");
  var cursor = { str: n, pos: 0 };
  var year = this._parseIsoItem(cursor, 4);
  var month = this._parseIsoItem(cursor);
  var day = this._parseIsoItem(cursor);
  var dt = /* @__PURE__ */ new Date();
  dt.setUTCFullYear(year);
  dt.setUTCMonth(month - 1);
  dt.setUTCDate(day);
  dt.setUTCHours(0);
  dt.setUTCMinutes(0);
  dt.setUTCSeconds(0);
  dt.setUTCMilliseconds(0);
  var days2 = Math.floor(dt.getTime() / 864e5);
  if (cursor.pos < cursor.str.length && cursor.str[cursor.pos] == "T")
    cursor.pos++;
  var hour = this._parseIsoItem(cursor);
  var min = this._parseIsoItem(cursor);
  var sec = this._parseIsoItem(cursor);
  var secs = (hour * 60 + min) * 60 + sec;
  if (cursor.pos < cursor.str.length && cursor.str[cursor.pos] == ".")
    cursor.pos++;
  var nanos = this._parseIsoItem(cursor, 9, 1e8);
  var tz = cursor.str.substr(cursor.pos);
  return new Instant(days2, secs * 1e3 * 1e3 * 1e3 + nanos);
};
Calendar.prototype.parseIsoDateTime.nfargs = function() {
  return 1;
};
Calendar.prototype._methods = function() {
  return {
    "isoDateTime": Calendar.prototype.isoDateTime,
    "parseIsoDateTime": Calendar.prototype.parseIsoDateTime
  };
};

// src/main/javascript/runtime/env.js
var ZiIdURI = function(s) {
  this.uri = s;
};
ZiIdURI.fromWire = function(cx2, om2, fields2) {
  return new ZiIdURI(fields2["uri"]);
};
ZiIdURI.prototype._towire = function(wf) {
  wf._wireable = "org.ziniki.common.ZiIdURI";
  wf.uri = this.uri;
};
var CommonEnv = function(bridge, broker) {
  if (!bridge)
    return;
  this.contracts = broker.contracts;
  this.structs = {};
  this.structs["Link"] = Link2;
  this.objects = {};
  this.objects["Random"] = Random;
  this.objects["FLBuiltin"] = FLBuiltin;
  this.objects["Crobag"] = Crobag2;
  this.objects["CroEntry"] = CroEntry;
  this.objects["Html"] = Html;
  this.objects["Image"] = Image2;
  this.objects["org.ziniki.common.ZiIdURI"] = ZiIdURI;
  this.objects["org.flasck.jvm.builtin.Crobag"] = Crobag2;
  this.objects["org.flasck.jvm.builtin.CroEntry"] = CroEntry;
  this.objects["Calendar"] = Calendar;
  this.logger = bridge;
  this.broker = broker;
  this.nextDivId = 1;
  this.divSince = this.nextDivId;
  this.evid = 1;
  this.cards = [];
  this.queue = [];
  this.namedSubscriptions = /* @__PURE__ */ new Map();
  this.unnamedSubscriptions = /* @__PURE__ */ new Map();
  this.subDag = /* @__PURE__ */ new Map();
  this.singletons = {};
  if (bridge.lock)
    this.locker = bridge;
  else
    this.locker = { lock: function() {
    }, unlock: function() {
    } };
};
CommonEnv.prototype.makeReady = function() {
  this.broker.register("Repeater", new ContainerRepeater());
};
CommonEnv.prototype.clear = function() {
  document.body.innerHTML = "";
};
CommonEnv.prototype.queueMessages = function(_cxt, msg) {
  this.locker.lock();
  this.queue.push(msg);
  var self = this;
  setTimeout(() => {
    self.dispatchMessages(_cxt);
    this.locker.unlock();
  }, 0);
};
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
  set.forEach((card) => {
    if (card._updateDisplay)
      card._updateDisplay(_cxt, card._renderTree);
    if (card._resizeDisplayElements)
      card._resizeDisplayElements(_cxt, card._renderTree);
  });
};
CommonEnv.prototype.handleMessages = function(_cxt, msg) {
  var msg = _cxt.full(msg);
  this.handleMessagesWith(_cxt, msg);
};
CommonEnv.prototype.handleMessagesWith = function(_cxt, msg) {
  msg = _cxt.full(msg);
  if (!msg)
    ;
  else if (msg instanceof FLError2 || typeof msg == "string") {
    _cxt.log(msg);
  } else if (msg instanceof Array) {
    for (var i = 0; i < msg.length; i++) {
      this.handleMessages(_cxt, msg[i]);
    }
  } else if (msg) {
    var ic = _cxt.split();
    ic.updateCards = _cxt.updateCards;
    try {
      var m = msg.dispatch(ic);
      this.handleMessages(_cxt, m);
    } catch (e) {
      _cxt.log(e.message);
      if (this.error) {
        this.error(e.toString());
      }
    }
  }
};
CommonEnv.prototype.addAll = function(ret2, m) {
  if (m) {
    if (Array.isArray(m)) {
      m.forEach((x) => this.addAll(ret2, x));
    } else
      ret2.push(m);
  }
};
CommonEnv.prototype.newContext = function() {
  return new FLContext(this, this.broker);
};
CommonEnv.prototype.unsubscribeAll = function(_cxt, card) {
  this.unnamedSubscriptions.forEach((v) => {
    for (var i = 0; i < v.length; i++) {
      this.broker.cancel(_cxt, v[i]._ihid);
    }
  });
  this.unnamedSubscriptions.clear();
  this.namedSubscriptions.forEach((forcxt) => {
    forcxt.forEach((v) => {
      this.broker.cancel(_cxt, v._ihid);
    });
  });
  this.namedSubscriptions.clear();
};
CommonEnv.prototype.getSingleton = function(name) {
  return this.singletons[name];
};
CommonEnv.prototype.cacheSingleton = function(name, value) {
  this.singletons[name] = value;
};
if (typeof window !== "undefined") {
  window.addEventListener("resize", function(ev) {
    if (window.appl) {
      var keys = Object.keys(window.appl.cards);
      for (var i = 0; i < keys.length; i++) {
        var card = window.appl.cards[keys[i]];
        card._resizeDisplayElements(env.newContext(), card._renderTree);
      }
    }
  });
}

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
  return function(cx2, ...rest) {
    self.serviceMethod(cx2, meth, rest);
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
MockContract.prototype._areYouA = function(cx2, ty) {
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
SubscriptionFor.prototype.cancel = function(cx2) {
  cx2.env.cancelBound(this.varName, this.handlerName);
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
var MockCard = function(cx2, card) {
  this.card = card;
  const newdiv = document.createElement("div");
  newdiv.setAttribute("id", cx2.nextDocumentId());
  document.body.appendChild(newdiv);
  this.card._renderInto(cx2, newdiv);
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
var ExplodingIdempotentHandler = function(cx2) {
  this.cx = cx2;
  this.successes = { expected: 0, actual: 0 };
  this.failures = [];
};
ExplodingIdempotentHandler.prototype = new IdempotentHandler();
ExplodingIdempotentHandler.prototype.constructor = ExplodingIdempotentHandler;
ExplodingIdempotentHandler.prototype.success = function(cx2) {
  cx2.log("success");
};
ExplodingIdempotentHandler.prototype.failure = function(cx2, msg) {
  cx2.log("failure: " + msg);
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
var UTContext = function(env2, broker) {
  FLContext.call(this, env2, broker);
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
  const ret2 = new MockContract(contract);
  this.broker.register(contract.name(), ret2);
  return ret2;
};
UTContext.prototype.mockAgent = function(agent) {
  return this.env.mockAgent(this, agent);
};
UTContext.prototype.mockCard = function(name, card) {
  return this.env.mockCard(this, name, card);
};
UTContext.prototype.explodingHandler = function() {
  const ret2 = new ExplodingIdempotentHandler(this);
  return ret2;
};
UTContext.prototype.mockHandler = function(contract) {
  const ret2 = new MockHandler(contract);
  return ret2;
};

// src/main/javascript/unittest/runner.js
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
  sendTo.redraw = function(cx2) {
    sendTo.obj._updateTemplate(cx2, sendTo.rt, "mock", "result", fn, template, sendTo.obj, []);
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
    var ret2 = rt[name];
    if (ret2)
      return ret2;
    rt = rt["single"];
    if (rt == null)
      return null;
  }
};
UTRunner.prototype._nameOf = function(zone, pos) {
  if (pos == 0)
    return "card";
  var ret2 = "";
  for (var i = 0; i < pos; i++) {
    if (ret2)
      ret2 += ".";
    ret2 += zone[i][1];
  }
  return ret2;
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
  var ret2 = new MockCard(_cxt, card);
  this.mocks[name] = ret2;
  this.cards.push(ret2);
  return ret2;
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
  var cx2 = this.newContext();
  var msgs = this.zinBch.dispatch(cx2, json, null);
  this.logger.log("have messages", msgs);
  this.queueMessages(cx2, msgs);
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

// src/main/javascript/forjava/wsbridge.js
function WSBridge(host2, port2, testWrapper) {
  var self = this;
  this.testWrapper = testWrapper;
  this.runner = new UTRunner(this);
  this.currentTest = null;
  this.ws = new WebSocket("ws://" + host2 + ":" + port2 + "/bridge");
  this.requestId = 1;
  this.sending = [];
  this.lockedOut = [];
  this.responders = {};
  this.ws.addEventListener("open", (ev) => {
    console.log("connected", ev);
    while (this.sending.length > 0) {
      var v = this.sending.shift();
      this.ws.send(v);
    }
  });
  this.ws.addEventListener("message", (ev) => {
    console.log("message", ev.data);
    var msg = JSON.parse(ev.data);
    var action = msg.action;
    if (action == "response") {
      var rid = msg.respondingTo;
      if (!this.responders[rid]) {
        console.log("there is nobody willing to handle response " + rid);
        return;
      }
      this.responders[rid].call(this, msg);
      delete this.responders[rid];
    } else {
      if (!WSBridge.handlers[action]) {
        console.log("there is no handler for " + action);
        return;
      }
      WSBridge.handlers[action].call(self, msg);
    }
  });
}
WSBridge.handlers = {};
WSBridge.prototype.log = function(...args) {
  console.log.apply(console.log, args);
};
WSBridge.prototype.debugmsg = function(...args) {
  console.log.apply(console.log, args);
};
WSBridge.prototype.module = function(runner2, moduleName) {
  this.runner = runner2;
  this.send({ action: "module", "name": moduleName });
  this.lock("bindModule");
  return "must-wait";
};
WSBridge.handlers["haveModule"] = function(msg) {
  var name = msg.name;
  var clz = window[msg.clz];
  var conn = msg.conn;
  console.log("have connection for module", this, name, clz);
  this.runner.bindModule(name, new clz(this, conn));
  this.unlock("haveModule");
};
WSBridge.handlers["prepareTest"] = function(msg) {
  console.log("run unit test", msg);
  var cxt = this.runner.newContext();
  var utf = this.testWrapper[msg.testname];
  this.currentTest = new utf(this.runner, cxt);
  console.log(this.currentTest);
  debugger;
  var steps = this.currentTest.dotest.call(this.currentTest, cxt);
  console.log(steps);
  this.send({ action: "steps", steps });
};
WSBridge.handlers["runStep"] = function(msg) {
  console.log("run unit test step", msg);
  var cxt = this.runner.newContext();
  var step = this.currentTest[msg.step];
  debugger;
  step.call(this.currentTest, cxt);
  this.unlock();
};
WSBridge.prototype.send = function(json) {
  var text = JSON.stringify(json);
  if (this.ws.readyState == this.ws.OPEN)
    this.ws.send(text);
  else
    this.sending.push(text);
};
WSBridge.prototype.connectToZiniki = function(wsapi, cb) {
  runner.broker.connectToServer("ws://" + host + ":" + port);
};
WSBridge.prototype.executeSync = function(runner2, st, cxt, steps) {
  this.runner = runner2;
  this.st = st;
  this.runcxt = cxt;
  this.readysteps = steps;
  this.unlock("ready to go");
};
WSBridge.prototype.nextRequestId = function(hdlr) {
  this.responders[this.requestId] = hdlr;
  return this.requestId++;
};
WSBridge.prototype.lock = function() {
  this.send({ action: "lock" });
};
WSBridge.prototype.unlock = function(msg) {
  this.send({ action: "unlock" });
};
export {
  JavaLogger,
  WSBridge
};

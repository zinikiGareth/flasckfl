// src/main/javascript/unittest/mocks.js
import { IdempotentHandler, NamedIdempotentHandler } from "/js/ziwsh.js";
import { FLError } from "/js/flasjs.js";
import { FLURI } from "/js/flasjs.js";
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
var MockAgent = function(agent) {
  this.agent = agent;
};
MockAgent.prototype.sendTo = function(_cxt, contract, msg, args) {
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
var MockHandler = function(ctr) {
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
MockHandler.prototype = new ExplodingIdempotentHandler();
MockHandler.prototype.constructor = MockHandler;
MockHandler.prototype._areYouA = MockContract.prototype._areYouA;
MockHandler.prototype.expect = MockContract.prototype.expect;
MockHandler.prototype.serviceMethod = MockContract.prototype.serviceMethod;
MockHandler.prototype.assertSatisfied = MockContract.prototype.assertSatisfied;
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

// src/main/javascript/unittest/runner.js
import { CommonEnv } from "/js/flasjs.js";

// src/main/javascript/unittest/utcxt.js
import { FLContext } from "/js/flasjs.js";
import { FLObject } from "/js/flasjs.js";
import { ResponseWithMessages } from "/js/flasjs.js";
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
import { SimpleBroker, JsonBeachhead, IdempotentHandler as IdempotentHandler2, NamedIdempotentHandler as NamedIdempotentHandler2 } from "/js/ziwsh.js";
import { FLError as FLError2 } from "/js/flasjs.js";
import { Debug, Send, Assign, ResponseWithMessages as ResponseWithMessages2, UpdateDisplay } from "/js/flasjs.js";
var UTRunner = function(bridge) {
  if (!bridge)
    bridge = console;
  CommonEnv.call(this, bridge, new SimpleBroker(bridge, this, {}));
  this.modules = {};
  this.moduleInstances = {};
  this.clear();
};
UTRunner.prototype = new CommonEnv();
UTRunner.prototype.constructor = UTRunner;
UTRunner.prototype.clear = function() {
  CommonEnv.prototype.clear.apply(this);
  this.toCancel = /* @__PURE__ */ new Map();
  this.errors = [];
  this.mocks = {};
  this.appls = [];
  this.activeSubscribers = [];
};
UTRunner.prototype.newContext = function() {
  return new UTContext(this, this.broker);
};
UTRunner.prototype.bindModule = function(name, jsm) {
  this.moduleInstances[name] = jsm;
};
UTRunner.prototype.makeReady = function() {
  CommonEnv.prototype.makeReady.call(this);
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
  if (inv instanceof Send)
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
  var ds = this.divSince;
  this.divSince = this.nextDivId;
  if (cnt != null) {
    if (cnt != this.nextDivId - ds) {
      throw Error("NEWDIV\n  expected: " + cnt + "\n  actual:   " + (this.nextDivId - ds));
    }
  }
};
UTRunner.prototype.expectCancel = function(handler) {
  var hn;
  if (handler instanceof NamedIdempotentHandler2) {
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
UTRunner.prototype.addHistory = function(state, title, url) {
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
  BoundVar,
  STSecurityModule,
  UTRunner
};

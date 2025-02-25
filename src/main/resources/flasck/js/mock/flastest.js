// src/main/javascript/unittest/mocks.js
import { IdempotentHandler, NamedIdempotentHandler } from "/js/ziwsh.js";
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
MockFLObject.prototype._isMock = function() {
  return true;
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
MockCard.prototype._isMock = function() {
  return true;
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
import { FLError } from "/js/flasjs.js";
import { Debug, Send, Assign, ResponseWithMessages as ResponseWithMessages2, UpdateDisplay } from "/js/flasjs.js";
var UTRunner = function(bridge) {
  if (!bridge)
    bridge = console;
  CommonEnv.call(this, bridge, new SimpleBroker(bridge, this, {}));
  this.modules = {};
  this.moduleInstances = {};
  this.expectCards = [];
  this.clear();
};
UTRunner.prototype = new CommonEnv();
UTRunner.prototype.constructor = UTRunner;
UTRunner.prototype.clear = function() {
  CommonEnv.prototype.clear.apply(this);
  this.toCancel = /* @__PURE__ */ new Map();
  this.expectCards = [];
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
    if (a instanceof FLError)
      a = a.message;
    throw new Error("NSV\n  expected: " + e + "\n  actual:   " + a);
  }
};
UTRunner.prototype.assertIdentical = function(_cxt, e, a) {
  e = _cxt.full(e);
  a = _cxt.full(a);
  if (a !== e) {
    if (a instanceof FLError)
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
  dest._destroy(_cxt);
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
  if (obj._isMock)
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
UTRunner.prototype.matchRoute = function(_cxt, expected) {
  console.log("matching route with", this._currentRoute, expected);
  if (this._currentRoute.pathname !== expected) {
    throw new Error("MATCH\n  expected: " + expected + "\n  actual:   " + this._currentRoute.pathname);
  }
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
UTRunner.prototype.expectCardCreation = function(_cxt, type, storeAs) {
  _cxt.log("expecting card to be created of type", type, "and will store as", storeAs);
  this.expectCards.push({ type, storeAs });
};
UTRunner.prototype.expectCardClose = function(_cxt, card) {
  _cxt.log("expecting card to be closed", card);
  this.expectCards.push({ card });
};
UTRunner.prototype.createRoutingCard = function(card) {
  for (var i = 0; i < this.expectCards.length; i++) {
    var x = this.expectCards[i];
    if (x.type && card.name() == x.type) {
      x.storeAs.bindActual(card);
      this.expectCards.splice(i, 1);
      break;
    }
  }
};
UTRunner.prototype.closeRoutingCard = function(card) {
  for (var i = 0; i < this.expectCards.length; i++) {
    var x = this.expectCards[i];
    if (x.card && card == x.card) {
      this.expectCards.splice(i, 1);
      break;
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
  this._currentRoute = url;
};
UTRunner.prototype.replaceRoute = function(url) {
  this._currentRoute = url;
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
function WSBridge(host, port) {
  var self = this;
  this.unittests = {};
  this.systemtests = {};
  this.runner = new UTRunner(this);
  this.currentTest = null;
  this.ws = new WebSocket("ws://" + host + ":" + port + "/bridge");
  this.requestId = 1;
  this.sending = [];
  this.lockedOut = [];
  this.responders = {};
  this.moduleCreators = {};
  this.ws.addEventListener("open", (ev) => {
    console.log("wsbridge connected");
    while (this.sending.length > 0) {
      var v = this.sending.shift();
      console.log("bridge =>", v);
      this.ws.send(v);
    }
  });
  this.ws.addEventListener("message", (ev) => {
    console.log("bridge <=", ev.data);
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
WSBridge.prototype.addUnitTest = function(name, test) {
  console.log("adding unit test", name);
  this.unittests[name] = test;
};
WSBridge.prototype.addSystemTest = function(name, test) {
  console.log("adding system test", name);
  this.systemtests[name] = test;
};
WSBridge.prototype.error = function(...args) {
  console.log.apply(console.log, args);
  if (this.ws) {
    this.send({ action: "error", error: merge(args) });
  }
};
WSBridge.prototype.log = function(...args) {
  console.log.apply(console.log, args);
  if (this.ws) {
    this.send({ action: "log", message: merge(args) });
  }
};
WSBridge.prototype.debugmsg = function(...args) {
  console.log.apply(console.log, args);
  this.send({ action: "debugmsg", message: merge(args) });
};
var merge = function(args) {
  var ret = "";
  var sep = "";
  for (var i = 0; i < args.length; i++) {
    ret += sep + args[i];
    sep = " ";
  }
  return ret;
};
WSBridge.prototype.connectModule = function(moduleName, callback) {
  console.log("creating module", moduleName);
  this.moduleCreators[moduleName] = callback;
  this.lock("bindModule");
  this.send({ action: "module", "name": moduleName });
  return "must-wait";
};
WSBridge.handlers["haveModule"] = function(msg) {
  console.log("have module", msg.name);
  var name = msg.name;
  var cb = this.moduleCreators[name];
  if (cb) {
    cb(this.runner, msg.conn);
  }
  delete this.moduleCreators[name];
  this.unlock("haveModule");
};
WSBridge.handlers["prepareUnitTest"] = function(msg) {
  console.log("UNIT", msg.wrapper, msg.testname);
  var cxt = this.runner.newContext();
  var utf = this.unittests[msg.wrapper][msg.testname];
  this.currentTest = new utf(this.runner, cxt);
  this.runner.clear();
  var steps = this.currentTest.dotest.call(this.currentTest, cxt);
  this.send({ action: "steps", steps });
};
WSBridge.handlers["prepareSystemTest"] = function(msg) {
  console.log("SYSTEST", msg.testclz);
  var cxt = this.runner.newContext();
  var stc = this.systemtests[msg.testclz];
  this.currentTest = new stc(this.runner, cxt);
  this.runner.clear();
  this.send({ action: "systemTestPrepared" });
};
WSBridge.handlers["prepareStage"] = function(msg) {
  console.log("PREPARE STAGE", msg.stage);
  var cxt = this.runner.newContext();
  var stage = this.currentTest[msg.stage];
  var steps = stage(cxt);
  this.send({ action: "steps", steps });
};
WSBridge.handlers["runStep"] = function(msg) {
  console.log("RUN STEP", msg.step);
  try {
    var cxt = this.runner.newContext();
    var step = this.currentTest[msg.step];
    step.call(this.currentTest, cxt);
  } catch (e) {
    console.log(e);
    this.error(e.toString());
  }
  this.unlock("runstep");
};
WSBridge.handlers["assertSatisfied"] = function(msg) {
  console.log("assert all expectations satisfied", msg);
  try {
    this.runner.assertSatisfied();
    this.runner.checkAtEnd();
  } catch (e) {
    console.log(e);
    this.error(e.toString());
  }
  this.unlock("assertSatisfied");
};
WSBridge.prototype.send = function(json) {
  var text = JSON.stringify(json);
  if (this.ws.readyState == this.ws.OPEN) {
    if (json.action != "log") {
      console.log("bridge =>", text);
    }
    this.ws.send(text);
  } else
    this.sending.push(text);
};
WSBridge.prototype.executeSync = function(runner, st, cxt, steps) {
  this.runner = runner;
  this.st = st;
  this.runcxt = cxt;
  this.readysteps = steps;
  this.unlock("ready to go");
};
WSBridge.prototype.nextRequestId = function(hdlr) {
  this.responders[this.requestId] = hdlr;
  return this.requestId++;
};
WSBridge.prototype.lock = function(msg) {
  console.log("lock", msg);
  this.send({ action: "lock", msg });
};
WSBridge.prototype.unlock = function(msg) {
  console.log("unlock", msg);
  this.send({ action: "unlock", msg });
};

// src/main/javascript/unittest/exposeme.js
function exposeTests(into) {
  into.bridge = new WSBridge("localhost", 14040);
  into.runner = into.bridge.runner;
  into.unittest = async function(holder, which) {
    into.testing = {};
    var imptest = await import("/js/" + holder + ".js");
    var elt = holder.replaceAll(".", "__");
    var ut = imptest[elt];
    if (typeof which === "undefined") {
      for (var t of Object.keys(ut)) {
        if (t.startsWith("_ut")) {
          console.log(" * " + t);
        }
      }
      return;
    }
    if (which.startsWith && which.startsWith("_ut")) {
    } else {
      which = "_ut" + which;
    }
    var utc = ut[which];
    var cxt = into.runner.newContext();
    into.testing.test = new utc(into.runner, cxt);
    into.bridge.currentTest = into.testing.test;
    into.runner.clear();
    into.testing.steps = into.testing.test.dotest(cxt);
    for (var s of into.testing.steps) {
      console.log(" * " + s);
    }
  };
  into.systest = async function(name, ...modules) {
    into.testing = {};
    var imptest = await import("/js/" + name + ".js");
    var elt = name.replaceAll(".", "__");
    var stc = imptest[elt];
    var cxt = into.runner.newContext();
    into.testing.test = new stc(into.runner, cxt);
    into.bridge.currentTest = into.testing.test;
    for (var m of modules) {
      var impmod = await import("/js/" + m + ".js");
      var installer = impmod.installer;
      installer(into.bridge);
    }
    into.runner.clear();
    into.testing.methods = figureSystemMethods(into.testing.test);
  };
  into.runto = function(tostep) {
    if (into.testing.steps) {
      unitRun(into.bridge, into.testing, tostep);
    } else {
      systemRun(into.bridge, into.testing, tostep);
    }
  };
}
function figureSystemMethods(inTest) {
  var methods = Object.keys(Object.getPrototypeOf(inTest)).sort();
  var ret = [];
  for (var s of methods) {
    if (s.startsWith("configure_step") || s.match("stage[0-9]*_step") || s.startsWith("finally_step")) {
      console.log(" * " + s);
      ret.push(s);
    }
  }
  return ret;
}
function unitRun(bridge, te, tostep) {
  if (te.amAt >= te.steps.length) {
    console.log("test complete; restart to rerun");
    return;
  }
  var untilStep = figureUnitStep(te.steps, tostep);
  if (untilStep == null) {
    return;
  }
  if (!te.amAt) {
    te.amAt = 0;
  }
  if (te.amAt > untilStep) {
    console.log("cannot go back in time; restart test to do that; already at", te.steps[te.amAt]);
    return;
  }
  var steps = [];
  while (te.amAt <= untilStep && te.amAt < te.steps.length) {
    steps.push(te.steps[te.amAt++]);
  }
  bridge.send({ action: "steps", steps });
}
function systemRun(bridge, te, tostep) {
  if (te.amAt >= te.methods.length) {
    console.log("test complete; restart to rerun");
    return;
  }
  var untilStep = figureSystemStep(te.methods, tostep);
  if (untilStep == null) {
    return;
  }
  if (!te.amAt) {
    te.amAt = 0;
  }
  if (te.amAt > untilStep) {
    console.log("cannot go back in time; restart test to do that; already at", te.methods[te.amAt]);
    return;
  }
  var steps = [];
  while (te.amAt <= untilStep) {
    steps.push(te.methods[te.amAt++]);
  }
  bridge.send({ action: "steps", steps });
}
function figureUnitStep(steps, step) {
  if (typeof step === "undefined") {
    return steps.length - 1;
  }
  return Number(step) - 1;
}
function figureSystemStep(steps, nickname) {
  var stepName = null;
  var endOf = null;
  var mg = null;
  if (nickname == "c") {
    endOf = "configure";
  } else if (nickname.match(/^c\.?[0-9]*$/)) {
    stepName = "configure_step_" + nickname.replace("c.", "");
  } else if (nickname.match(/^[0-9]+$/)) {
    endOf = "stage" + nickname;
  } else if (mg = nickname.match(/^([0-9]+)\.([0-9]+)$/)) {
    stepName = "stage" + mg[1] + "_step_" + mg[2];
  } else if (nickname.match(/^f\.[0-9]+$/)) {
    stepName = "finally_step_" + nickname.replace("f.", "");
  } else if (nickname == "f") {
    return steps.length - 1;
  } else {
    stepName = nickname;
  }
  var matchEnd = null;
  for (var i = 0; i < steps.length; i++) {
    var s = steps[i];
    if (s == stepName) {
      return i;
    } else if (endOf && s.startsWith(endOf)) {
      matchEnd = i;
    }
  }
  if (matchEnd != null) {
    return matchEnd;
  }
  console.log("no step matched", nickname);
  return null;
}
export {
  BoundVar,
  STSecurityModule,
  UTRunner,
  exposeTests
};

// src/main/javascript/runtime/error.js
var FLError = class _FLError extends Error {
  constructor(msg) {
    super(msg);
    this.name = "FLError";
  }
  _compare(_cxt, other) {
    if (!(other instanceof _FLError))
      return false;
    if (other.message != this.message)
      return false;
    return true;
  }
  _throw() {
    return true;
  }
  _updateTemplate(_cxt) {
    _cxt.log("error: " + this.message);
    return this;
  }
};
FLError.eval = function(_cxt, msg) {
  return new FLError(msg);
};

// src/main/javascript/runtime/messages.js
import { IdempotentHandler, NamedIdempotentHandler } from "/js/ziwsh.js";

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
  if (cp instanceof FLError)
    return cp;
  else if (!cp)
    return [hd];
  cp = cp.slice(0);
  cp.splice(0, 0, hd);
  return cp;
};
var AssignItem = function(list, n) {
  this.list = list;
  this.n = n;
};
AssignItem.prototype._field_head = function(_cxt) {
  return this.list[this.n];
};
AssignItem.prototype._field_head.nfargs = function() {
  return 0;
};
AssignItem.prototype.set = function(obj) {
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
var Send = function() {
};
Send.eval = function(_cxt, obj, meth, args, handle, subscriptionName) {
  const s = new Send();
  s.subcontext = _cxt.subcontext;
  if (obj instanceof NamedIdempotentHandler) {
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
Send.prototype._full = function(cx2) {
  this.obj = cx2.full(this.obj);
  this.meth = cx2.full(this.meth);
  this.args = cx2.full(this.args);
  this.handle = cx2.full(this.handle);
  this.subscriptionName = cx2.full(this.subscriptionName);
};
Send.prototype._compare = function(cx2, other) {
  if (other instanceof Send) {
    return cx2.compare(this.obj, other.obj) && cx2.compare(this.meth, other.meth) && cx2.compare(this.args, other.args);
  } else
    return false;
};
Send.prototype.dispatch = function(cx2) {
  this._full(cx2);
  if (this.obj instanceof FLError) {
    cx2.log(this.obj);
    return null;
  }
  if (this.obj instanceof ResponseWithMessages) {
    const ret3 = ResponseWithMessages.messages(cx2, this.obj);
    ret3.push(Send.eval(cx2, ResponseWithMessages.response(cx2, this.obj), this.meth, this.args, this.handle));
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
    hdlr = new NamedIdempotentHandler(this.handle, this.subscriptionName);
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
Send.prototype.toString = function() {
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
  if (this.expr instanceof ResponseWithMessages) {
    msgs.unshift(ResponseWithMessages.messages(cx2, this.expr));
    this.expr = ResponseWithMessages.response(cx2, this.expr);
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
  if (!(target instanceof AssignItem)) {
    throw Error("No, it needs to be an Item");
  }
  if (this.expr instanceof ResponseWithMessages) {
    msgs.unshift(ResponseWithMessages.messages(cx2, this.expr));
    this.expr = ResponseWithMessages.response(cx2, this.expr);
  }
  target.set(this.expr);
  return msgs;
};
AssignCons.prototype.toString = function() {
  return "AssignCons[]";
};
var ResponseWithMessages = function(cx2, obj, msgs) {
  this.obj = obj;
  this.msgs = msgs;
};
ResponseWithMessages.prototype._full = function(cx2) {
  this.obj = cx2.full(this.obj);
  this.msgs = cx2.full(this.msgs);
};
ResponseWithMessages.response = function(cx2, rwm) {
  if (rwm instanceof ResponseWithMessages)
    return rwm.obj;
  else
    return rwm;
};
ResponseWithMessages.messages = function(cx2, rwm) {
  if (rwm instanceof ResponseWithMessages)
    return rwm.msgs;
  else
    return null;
};
ResponseWithMessages.prototype.toString = function() {
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

// src/main/javascript/runtime/appl/route.js
var Segment = function(action, segment, map) {
  this.action = action;
  this.segment = segment;
  this.entry = map;
};
Segment.prototype.isdir = function() {
  return Object.keys(this.entry.namedPaths).length > 0 || this.entry.paramRoute != null;
};
Segment.prototype.toString = function() {
  return this.action + "->" + this.segment;
};
var Route = function() {
  this.parts = [];
  this.pos = 0;
};
Route.prototype.toString = function() {
  return this.parts.toString();
};
Route.parse = function(baseuri, table, path) {
  if (typeof path === "string") {
    try {
      var p1 = new URL(path);
      path = p1;
    } catch (e) {
      try {
        var p2 = new URL(baseuri + path);
        path = p2;
      } catch (f) {
        var p3;
        if (path.includes("#"))
          p3 = new URL("https://base.uri/" + path);
        else if (path.includes("?"))
          p3 = new URL("https://base.uri/" + path + "#/");
        else
          p3 = new URL("https://base.uri/#" + path);
        path = p3;
      }
    }
  } else if (path instanceof Location) {
    path = new URL(path.href);
  } else if (!(path instanceof URL)) {
    throw new Error("path is not a url, location or string");
  }
  var claimedRoute = path;
  var query = new URLSearchParams(path.search);
  if (path.hash) {
    path = path.hash.replace(/^#/, "");
  } else if (baseuri || typeof baseuri === "string") {
    var buu = baseuri;
    if (typeof buu == "string") {
      try {
        buu = new URL(buu);
        buu = buu.path;
      } catch (e) {
      }
    } else if (buu instanceof URL) {
      buu = buu.path;
    } else {
      throw new Error("baseuri is not a URL or a string");
    }
    if (!buu)
      buu = "";
    if (path && path.pathname)
      path = path.pathname.replace(buu, "");
    else
      path = "";
  } else {
    path = "";
  }
  var route;
  route = path.split("/").filter((i) => i);
  var ret2 = new Route();
  ret2.claimedRoute = claimedRoute;
  ret2.parts.push(new Segment("push", "/", table));
  var map = table;
  var tmp = "/";
  for (var s of route) {
    var next = map.route(s);
    if (!next) {
      console.log("there is no entry in the routing table for", s, "in", next);
      break;
    }
    if (tmp.length > 1) {
      tmp += "/";
    }
    tmp += s;
    ret2.parts.push(new Segment("push", s, next));
    map = next;
  }
  ret2.claimedRoute.pathname = tmp;
  ret2.query = query;
  return ret2;
};
Route.prototype.reset = function() {
  this.pos = 0;
};
Route.prototype.length = function() {
  return this.parts.length - this.pos;
};
Route.prototype.head = function() {
  return this.parts[this.pos];
};
Route.prototype.advance = function() {
  this.pos++;
};
Route.prototype.movingFrom = function(from) {
  this.reset();
  if (from)
    from.reset();
  var ret2 = new Route();
  ret2.claimedRoute = this.claimedRoute;
  ret2.query = this.query;
  var popAt = null;
  while (from && this.length() > 0 && from.length() > 0) {
    popAt = from.head();
    if (this.head().segment != from.head().segment)
      break;
    this.advance();
    from.advance();
  }
  while (from && from.length() > 0) {
    var s = from.head();
    ret2.parts.unshift(new Segment("pop", s.segment, s.entry));
    from.advance();
  }
  while (this.length() > 0) {
    popAt = this.head();
    ret2.parts.push(this.head());
    this.advance();
  }
  if (popAt != null && ret2.parts.length > 0) {
    ret2.parts.push(new Segment("at", popAt.segment, popAt.entry));
  }
  return ret2;
};
Route.prototype.getQueryParam = function(v) {
  return this.query.get(v);
};

// src/main/javascript/runtime/appl/routeevent.js
var RouteTraversalState = function(appl, allDone) {
  this.appl = appl;
  this.newcards = [];
  this.allDone = allDone;
};
var RouteEvent = function(route, stateOrAppl, lastAct, posn, allDone) {
  this.route = route;
  this.nextAction(route.head().entry, lastAct, posn);
  if (stateOrAppl instanceof RouteTraversalState)
    this.state = stateOrAppl;
  else
    this.state = new RouteTraversalState(stateOrAppl, allDone);
};
RouteEvent.prototype.dispatch = function(cxt) {
  if (this.route.length() == 0) {
    return;
  }
  var needPause = null;
  switch (this.route.head().action) {
    case "push": {
      needPause = this.processDownAction(cxt);
      break;
    }
    case "pop": {
      this.processUpAction(cxt);
      break;
    }
    case "at": {
      this.processAtAction(cxt);
      break;
    }
  }
  if (needPause != "break")
    this.queueNextAction(cxt);
};
RouteEvent.prototype.processDownAction = function(cxt) {
  switch (this.action) {
    case "param": {
      var p = this.route.head().entry.param;
      if (p) {
        var q = this.route.head().segment;
        this.state.appl.bindParam(cxt, p, q);
      }
      break;
    }
    case "title": {
      if (this.route.head().entry.title) {
        this.state.appl.setTitle(cxt, this.route.head().entry.title);
      }
      break;
    }
    case "secure": {
      var e = this.route.head();
      if (this.route.head().entry.secure) {
        var nev = new RouteEvent(this.route, this.state, this.action, null);
        this.state.appl.handleSecurity(cxt, nev);
        return "break";
      }
      break;
    }
    case "create": {
      for (var ci of this.route.head().entry.cards) {
        this.state.appl.createCard(cxt, ci);
        this.state.newcards.unshift(ci.name);
      }
      break;
    }
    case "enter": {
      var act = this.route.head().entry.enter[this.posn];
      var arg;
      if (act.contract == "Lifecycle" && act.action == "query") {
        arg = this.route.getQueryParam(act.args[0].str);
      }
      this.state.appl.oneAction(cxt, act, arg);
      break;
    }
    case "at":
    case "exit":
    case "destroy": {
      break;
    }
    default: {
      throw new Error("cannot handle action " + this.action);
    }
  }
};
RouteEvent.prototype.processUpAction = function(cxt) {
  switch (this.action) {
    case "param":
    case "title":
    case "create":
    case "enter":
    case "secure":
    case "at": {
      break;
    }
    case "exit": {
      var act = this.route.head().entry.exit[this.posn];
      var arg;
      this.state.appl.oneAction(cxt, act, arg);
      break;
    }
    case "destroy": {
      for (var ci of this.route.head().entry.cards) {
        this.state.appl.destroyCard(cxt, ci);
      }
      break;
    }
    default: {
      throw new Error("cannot handle action " + this.action);
    }
  }
};
RouteEvent.prototype.processAtAction = function(cxt) {
  switch (this.action) {
    case "param":
    case "title":
    case "create":
    case "enter":
    case "exit":
    case "destroy":
    case "secure": {
      break;
    }
    case "at": {
      var act = this.route.head().entry.at[this.posn];
      var arg;
      if (act.contract == "Lifecycle" && act.action == "query") {
        arg = this.route.getQueryParam(act.args[0].str);
      }
      this.state.appl.oneAction(cxt, act, arg);
      break;
    }
    default: {
      throw new Error("cannot handle action " + this.action);
    }
  }
};
RouteEvent.prototype.queueNextAction = function(cxt) {
  var nev = new RouteEvent(this.route, this.state, this.action, this.posn);
  if (nev.action) {
    cxt.env.queueMessages(cxt, nev);
  } else {
    this.route.advance();
    if (this.route.length() > 0) {
      nev = new RouteEvent(this.route, this.state, null, null);
      cxt.env.queueMessages(cxt, nev);
    } else {
      this.alldone(cxt);
    }
  }
};
RouteEvent.prototype.alldone = function(cxt) {
  for (var c of this.state.newcards) {
    this.state.appl.readyCard(cxt, c);
  }
  var rn = this.route.claimedRoute;
  if (!rn.pathname.endsWith("/") && this.route.parts[this.route.parts.length - 1].isdir()) {
    rn.pathname += "/";
  }
  this.state.appl.complete(cxt, rn);
  if (this.state.allDone)
    this.state.allDone();
};
RouteEvent.prototype.nextAction = function(head, curr, posn) {
  switch (curr) {
    case null:
    case void 0:
      this.action = "param";
      break;
    case "param":
      this.action = "secure";
      break;
    case "secure":
      this.action = "title";
      break;
    case "title":
      this.action = "create";
      break;
    case "create":
      this.nextAction(head, "enter", null);
      break;
    case "enter": {
      if (head.enter.length == 0 || posn != null && posn + 1 >= head.enter.length) {
        this.nextAction(head, "exit", null);
      } else {
        this.action = "enter";
        this.posn = posn == null ? 0 : posn + 1;
      }
      break;
    }
    case "exit":
      if (head.exit.length == 0 || posn != null && posn + 1 >= head.exit.length) {
        this.nextAction(head, "at", null);
      } else {
        this.action = "exit";
        this.posn = posn == null ? 0 : posn + 1;
      }
      break;
    case "at":
      if (head.at.length == 0 || posn != null && posn + 1 >= head.at.length) {
        this.action = "destroy";
      } else {
        this.action = "at";
        this.posn = posn == null ? 0 : posn + 1;
      }
      break;
    case "destroy":
      this.action = null;
      break;
  }
};

// src/main/javascript/runtime/appl/routingentry.js
function RoutingEntry(entry) {
  this.secure = entry.secure;
  this.title = entry.title;
  this.namedPaths = {};
  this.paramRoute = null;
  this.path = entry.path;
  this.param = entry.param;
  this.cards = entry.cards;
  this.enter = entry.enter;
  this.at = entry.at;
  this.exit = entry.exit;
  for (var sub of entry.routes) {
    if (sub.path) {
      this.namedPaths[sub.path] = new RoutingEntry(sub);
    } else if (sub.param) {
      this.paramRoute = new RoutingEntry(sub);
    }
  }
}
RoutingEntry.prototype.route = function(path) {
  if (this.namedPaths[path]) {
    return this.namedPaths[path];
  } else if (this.paramRoute) {
    return this.paramRoute;
  } else
    return null;
};

// src/main/javascript/runtime/appl/appl.js
var Application = function(_cxt, topdiv, baseuri) {
  if (!_cxt)
    return;
  this._env = _cxt.env;
  this._env.appl = this;
  if (typeof topdiv == "string")
    this.topdiv = document.getElementById(topdiv);
  else
    this.topdiv = topdiv;
  this.baseuri = baseuri;
  this.cards = {};
  this.params = {};
  this.currentRoute = null;
  this.addResizeListener(_cxt.env);
};
Application.prototype.addResizeListener = function(env2) {
  if (typeof window === "undefined")
    return;
  var appl = this;
  window.addEventListener("resize", function(ev) {
    var keys = Object.keys(appl.cards);
    for (var i = 0; i < keys.length; i++) {
      var card = appl.cards[keys[i]];
      card._resizeDisplayElements(env2.newContext(), card._renderTree);
    }
  });
};
Application.prototype.baseUri = function(_cxt) {
  return this.baseuri;
};
Application.prototype.relativeRoute = function(_cxt, path, allDone) {
  var route = new URL(path, this.currentRoute);
  _cxt.env.addHistory({}, null, route);
  this.gotoRoute(_cxt, route, allDone);
};
Application.prototype.gotoRoute = function(_cxt, route, allDone) {
  _cxt.log("going to route", route, "from", this.currentRoute);
  var goto = Route.parse(this.baseUri(), new RoutingEntry(this._routing()), route);
  var curr = null;
  if (this.currentRoute) {
    curr = Route.parse(this.baseUri(), new RoutingEntry(this._routing()), this.currentRoute);
  }
  var moveTo = goto.movingFrom(curr);
  _cxt.log("move to is", moveTo);
  if (moveTo.head()) {
    var event = new RouteEvent(moveTo, this, null, null, allDone);
    _cxt.env.queueMessages(_cxt, event);
  }
};
Application.prototype.handleSecurity = function(_cxt, ev) {
  if (!this.securityModule.requireLogin(_cxt, this, this.topdiv)) {
    this.routingPendingSecure = ev;
  } else {
    _cxt.env.queueMessages(ev);
  }
};
Application.prototype.nowLoggedIn = function(_cxt) {
  if (this.routingPendingSecure instanceof RouteEvent)
    _cxt.env.queueMessages(_cxt, this.routingPendingSecure);
  this.routingPendingSecure = null;
};
Application.prototype.parseRoute = function(_cxt, r) {
  var buri;
  if (typeof baseUri !== "undefined" && baseUri)
    buri = baseUri;
  else
    buri = this.baseUri();
  if (r instanceof Location || r instanceof URL) {
    if (!buri) {
      r = r.hash;
    } else
      r = r.href;
  }
  if (buri) {
    if (r.startsWith("/"))
      r = buri + r;
    if (!r.endsWith("/"))
      r = r + "/";
    try {
      if (this.currentPath)
        r = new URL(r, this.currentPath).href;
      else
        r = new URL(r, this.baseUri()).href;
    } catch (e) {
    }
    this.currentPath = r;
  }
  var url = r.replace(buri, "").replace(/^[#/]*/, "");
  var parts = url.split("/").filter((x) => !!x);
  return parts;
};
Application.prototype.setTitle = function(_cxt, title) {
  if (title != null)
    this.title = title;
};
Application.prototype.complete = function(_cxt, route) {
  this.currentRoute = route;
  _cxt.env.queueMessages(_cxt, new UpdateDisplay(_cxt, this));
  _cxt.replaceRoute(this.currentRoute);
};
Application.prototype.bindParam = function(_cxt, param, value) {
  this.params[param] = value;
};
Application.prototype.createCard = function(_cxt, ci) {
  var card = this.cards[ci.name] = new ci.card(_cxt);
  _cxt.createRoutingCard(card);
  var ctr = _cxt.findContractOnCard(card, "Lifecycle");
  if (ctr && ctr.init) {
    var msgs = ctr.init(_cxt);
    _cxt.env.queueMessages(_cxt, msgs);
  }
};
Application.prototype.destroyCard = function(_cxt, ci) {
  var card = this.cards[ci.name];
  card._destroy(_cxt);
  _cxt.closeRoutingCard(card);
  card._updateDisplay(_cxt, card._renderTree);
};
Application.prototype.readyCard = function(_cxt, name) {
  var card = this.cards[name];
  var ctr = _cxt.findContractOnCard(card, "Lifecycle");
  if (ctr && ctr.ready) {
    var msgs = ctr.ready(_cxt);
    _cxt.env.queueMessages(_cxt, msgs);
  }
};
Application.prototype.oneAction = function(_cxt, act, arg) {
  var card = this.cards[act.card];
  var ctr = _cxt.findContractOnCard(card, act.contract);
  if (ctr) {
    var m = act.action;
    if (ctr[m]) {
      var callWith = [_cxt];
      for (var ai = 0; ai < act.args.length; ai++) {
        var aa = act.args[ai];
        if (aa.str) {
          callWith.push(aa.str);
        } else if (aa.ref) {
          callWith.push(this.cards[aa.ref]);
        } else if (aa.param) {
          callWith.push(this.params[aa.param]);
        } else if (aa.expr) {
          callWith.push(this[aa.expr].call(this, _cxt));
        } else
          throw new Error("huh? " + JSON.stringify(aa));
      }
      if (typeof arg !== "undefined") {
        callWith.push(arg);
      }
      var msgs = ctr[m].apply(ctr, callWith);
      _cxt.env.queueMessages(_cxt, msgs);
    }
  }
};
Application.prototype._currentRenderTree = function() {
  var card = this.cards["main"];
  if (card == null)
    return null;
  return card._currentRenderTree();
};
Application.prototype._updateDisplay = function(_cxt, rt) {
  _cxt.log("updating display");
  if (this.title) {
    var titles = document.head.getElementsByTagName("title");
    if (titles.length == 0) {
      var t = document.createElement("title");
      document.head.appendChild(t);
      titles = [t];
    }
    titles[0].innerText = this.title;
  }
  var card = this.cards["main"];
  if (card == null)
    return;
  if (card._renderTree == null) {
    this.cards["main"]._renderInto(_cxt, this.topdiv);
  }
  card._updateDisplay(_cxt, card._renderTree);
};

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
  return this.missing.length;
};
FLCurry.prototype.toString = function() {
  return "FLCurry[" + this.missing.length + "]";
};

// src/main/javascript/runtime/closure.js
var closNum = 1;
var FLClosure = function(obj, fn, args) {
  if (!fn)
    throw new Error("must define a function");
  this.label = "Clos#" + ++closNum;
  this.obj = obj;
  this.fn = fn;
  args.splice(0, 0, null);
  this.args = args;
};
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
  if (this.args.length < cnt + 1) {
    var xcs = {};
    for (var i = 1; i < this.args.length; i++) {
      xcs[i] = this.args[i];
    }
    return new FLCurry(this.obj, this.fn, cnt, xcs);
  }
  this.val = this.fn.apply(this.obj, this.args.slice(0, cnt + 1));
  var ret2 = this.val;
  if (this.val instanceof ResponseWithMessages) {
    this.val = ResponseWithMessages.response(_cxt, this.val);
  }
  if (cnt + 1 < this.args.length) {
    ret2 = this.val = new FLClosure(this.obj, this.val, this.args.slice(cnt + 1));
  }
  return ret2;
};
FLClosure.prototype.apply = function(_, args) {
  const asfn = this.eval(args[0]);
  if (asfn instanceof FLError)
    return asfn;
  return asfn.apply(null, args);
};
FLClosure.prototype.nfargs = function() {
  return 0;
};
FLClosure.prototype.toString = function() {
  return "FLClosure[]";
};

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
    return Send.eval(cx2, this.obj, this.meth, all, this.handler, this.subscriptionName);
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
import { EvalContext, FieldsContainer } from "/js/ziwsh.js";

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
var Tuple = function() {
};
Tuple.eval = function(_cxt, args) {
  const ret2 = new Tuple();
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
    return new FLError("no matching case");
  list = _cxt.spine(list);
  if (!Array.isArray(list))
    return new FLError("no matching case");
  if (n < 0 || n >= list.length)
    return new FLError("out of bounds");
  return list[n];
};
FLBuiltin.nth.nfargs = function() {
  return 2;
};
FLBuiltin.item = function(_cxt, n, list) {
  n = _cxt.full(n);
  if (typeof n != "number")
    return new FLError("no matching case");
  list = _cxt.spine(list);
  if (!Array.isArray(list))
    return new FLError("no matching case");
  if (n < 0 || n >= list.length)
    return new FLError("out of bounds");
  return new AssignItem(list, n);
};
FLBuiltin.item.nfargs = function() {
  return 2;
};
FLBuiltin.append = function(_cxt, list, elt) {
  list = _cxt.spine(list);
  if (!Array.isArray(list))
    return new FLError("no matching case");
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
    return new FLError("no matching case");
  list = _cxt.spine(list);
  if (!Array.isArray(list))
    return new FLError("no matching case");
  if (n < 0 || n >= list.length)
    return new FLError("out of bounds");
  var cp = list.slice(0);
  cp[n] = elt;
  return cp;
};
FLBuiltin.replace.nfargs = function() {
  return 3;
};
FLBuiltin.reverse = function(_cxt, list) {
  list = _cxt.spine(list);
  if (!Array.isArray(list))
    return new FLError("no matching case");
  var ret2 = new Array(list.length);
  for (var i = 0; i < list.length; i++) {
    ret2[list.length - i - 1] = list[i];
  }
  return ret2;
};
FLBuiltin.reverse.nfargs = function() {
  return 1;
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
  if (list instanceof FLError)
    return list;
  else if (!list)
    return [];
  quant = _cxt.full(quant);
  if (quant instanceof FLError)
    return quant;
  if (typeof quant !== "number")
    return new FLError("no matching case");
  if (list.length <= quant)
    return list;
  return list.slice(0, quant);
};
FLBuiltin.take.nfargs = function() {
  return 2;
};
FLBuiltin.drop = function(_cxt, quant, list) {
  list = _cxt.spine(list);
  if (list instanceof FLError)
    return list;
  else if (!list)
    return [];
  quant = _cxt.full(quant);
  if (quant instanceof FLError)
    return quant;
  if (typeof quant !== "number")
    return new FLError("no matching case");
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
FLBuiltin.numberFromString = function(_cxt, str) {
  str = _cxt.head(str);
  if (typeof str != "string")
    return _cxt.error("not a string");
  return parseFloat(str);
};
FLBuiltin.numberFromString.nfargs = function() {
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
  if (sh instanceof FLError)
    return sh;
  else if (sh.routes) {
    if (sh.routes[v] === void 0)
      return new FLError("there is no card bound to route var '" + v + "'");
    return sh.routes[v];
  } else if (sh.card) {
    sh = sh.card;
    sh._updateFromInputs();
  } else if (sh.agent)
    sh = sh.agent;
  if (sh.state.dict[v] === void 0)
    return new FLError("No field '" + v + "' in probe_state");
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
  if (msgs instanceof FLError)
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
var HashPair = function() {
};
HashPair.eval = function(_cxt, args) {
  var ret2 = new HashPair();
  ret2.m = args[0];
  ret2.o = args[1];
  return ret2;
};
FLBuiltin.hashPair = function(_cxt, key, value) {
  return HashPair.eval(_cxt, [key, value]);
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
    return new FLError("no member " + member);
};
FLBuiltin.assoc.nfargs = function() {
  return 2;
};
function FLURI(s) {
  this.uri = s;
}
FLURI.prototype.resolve = function(base) {
  return new URL(this.uri, base);
};
FLURI.prototype._towire = function(into) {
  into.uri = this.uri;
};
FLBuiltin.parseUri = function(_cxt, s) {
  s = _cxt.full(s);
  if (s instanceof FLError)
    return s;
  else if (typeof s !== "string")
    return new FLError("not a string");
  else
    return new FLURI(s);
};
FLBuiltin.parseUri.nfargs = function() {
  return 1;
};
FLBuiltin.parseJson = function(_cxt, s) {
  s = _cxt.full(s);
  if (s instanceof FLError)
    return s;
  return JSON.parse(s);
};
FLBuiltin.parseJson.nfargs = function() {
  return 1;
};

// src/main/javascript/runtime/flcxt.js
var FLContext = function(env2, broker) {
  EvalContext.call(this, env2, broker);
  this.subcontext = null;
};
FLContext.prototype = new EvalContext();
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
  return new FLClosure(null, fn, args);
};
FLContext.prototype.oclosure = function(fn, obj, ...args) {
  return new FLClosure(obj, fn, args);
};
FLContext.prototype.curry = function(reqd, fn, ...args) {
  var xcs = {};
  for (var i = 0; i < args.length; i++) {
    xcs[i + 1] = args[i];
  }
  return new FLCurry(null, fn, reqd, xcs);
};
FLContext.prototype.ocurry = function(reqd, fn, obj, ...args) {
  var xcs = {};
  for (var i = 0; i < args.length; i++) {
    xcs[i + 1] = args[i];
  }
  return new FLCurry(obj, fn, reqd, xcs);
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
  return new FLCurry(null, fn, reqd, xcs);
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
      return new FLError("member was not a hashpair");
    var m = this.full(hp.m);
    ret2[m] = hp.o;
  }
  return ret2;
};
FLContext.prototype.applyhash = function(basic, hash) {
  basic = this.head(basic);
  if (basic instanceof FLError)
    return basic;
  hash = this.spine(hash);
  if (hash instanceof FLError)
    return hash;
  var okh = Object.keys(hash);
  for (var i = 0; i < okh.length; i++) {
    var p = okh[i];
    if (!basic.state.has(p))
      return new FLError("cannot override member: " + p);
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
  return FLError.eval(this, msg);
};
FLContext.prototype.mksend = function(meth, obj, cnt, handler, subscriptionName) {
  if (cnt == 0)
    return Send.eval(this, obj, meth, [], handler, subscriptionName);
  else
    return new FLMakeSend(meth, obj, cnt, handler, subscriptionName);
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
  while (obj instanceof FLClosure) {
    obj = obj.eval(this);
    if (obj instanceof ResponseWithMessages) {
      this.env.queueMessages(this, ResponseWithMessages.messages(this, obj));
      obj = ResponseWithMessages.response(this, obj);
    }
  }
  return obj;
};
FLContext.prototype.spine = function(obj) {
  obj = this.head(obj);
  if (obj instanceof FLError)
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
      if (obj[i] instanceof ResponseWithMessages) {
        msgs.unshift(obj[i].msgs);
        obj[i] = obj[i].obj;
      }
    }
  } else if (obj.state instanceof FieldsContainer) {
    var ks = Object.keys(obj.state.dict);
    for (var i = 0; i < ks.length; i++) {
      var tmp = this.full(obj.state.dict[ks[i]]);
      if (tmp instanceof ResponseWithMessages) {
        msgs.unshift(tmp.msgs);
        tmp = tmp.obj;
      }
      obj.state.dict[ks[i]] = tmp;
    }
  }
  if (msgs.length)
    return new ResponseWithMessages(this, obj, msgs);
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
  } else if (left instanceof FLError && right instanceof FLError) {
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
  if (reply instanceof FLError) {
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
FLContext.prototype.replaceRoute = function(url) {
  this.env.replaceRoute(url);
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
FLContext.prototype.createRoutingCard = function(card) {
  if (this.env.createRoutingCard) {
    this.env.createRoutingCard(card);
  }
};
FLContext.prototype.closeRoutingCard = function(card) {
  if (this.env.closeRoutingCard) {
    this.env.closeRoutingCard(card);
  }
};

// src/main/javascript/runtime/repeater.js
var ContainerRepeater = function() {
};
ContainerRepeater.prototype.callMe = function(cx2, callback) {
  return Send.eval(cx2, callback, "call", []);
};

// src/main/javascript/runtime/html.js
var Html = function(_cxt, _html) {
  FLObject.call(this, _cxt);
  this.state = _cxt.fields();
  this.state.set("html", _html);
};
Html._ctor_from = function(_cxt, _card, _html) {
  var ret2;
  if (!_html._convertToHTML) {
    ret2 = new FLError("not valid HTML source");
  } else {
    ret2 = new Html(
      _cxt,
      _html._convertToHTML()
      /* state.get('body') */
    );
  }
  return new ResponseWithMessages(_cxt, ret2, []);
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

// src/main/javascript/runtime/link.js
var Link = function(_cxt) {
  this.state = _cxt.fields();
  this.state.set("_type", "Link");
};
Link._typename = "Link";
Link.prototype._areYouA = function(_cxt, ty) {
  if (_cxt.isTruthy(ty == "Link")) {
    return true;
  } else
    return false;
};
Link.prototype._areYouA.nfargs = function() {
  return 1;
};
Link.eval = function(_cxt, _uri, _title) {
  var v1 = new Link(_cxt);
  v1.state.set("uri", _uri);
  v1.state.set("title", _title);
  return v1;
};
Link.eval.nfargs = function() {
  return 2;
};
Link.prototype._field_title = function(_cxt) {
  return this.state.get("title");
};
Link.prototype._field_title.nfargs = function() {
  return 0;
};
Link.prototype._field_uri = function(_cxt) {
  return this.state.get("uri");
};
Link.prototype._field_uri.nfargs = function() {
  return 0;
};

// src/main/javascript/runtime/crobag.js
import { IdempotentHandler as IdempotentHandler2 } from "/js/ziwsh.js";
var SlideWindow = function(_cxt) {
  IdempotentHandler2.call(this, _cxt);
  return;
};
SlideWindow.prototype = new IdempotentHandler2();
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
  IdempotentHandler2.call(this, _cxt);
  return;
};
CrobagWindow.prototype = new IdempotentHandler2();
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
var Crobag = function(_cxt, _card) {
  FLObject.call(this, _cxt);
  this._card = _card;
  this.state = { dict: {} };
  this._entries = [];
};
Crobag._ctor_new = function(_cxt, _card) {
  const ret2 = new Crobag(_cxt, _card);
  return new ResponseWithMessages(_cxt, ret2, []);
};
Crobag._ctor_new.nfargs = function() {
  return 1;
};
Crobag.fromWire = function(cx2, om2, fields2) {
  var ret2 = new Crobag(cx2, null);
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
Crobag.prototype._towire = function(wf) {
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
Crobag.prototype.insert = function(_cxt, key, val) {
  return [CrobagChangeEvent.eval(_cxt, this, "insert", key, null, val)];
};
Crobag.prototype.insert.nfargs = function() {
  return 1;
};
Crobag.prototype.put = function(_cxt, key, val) {
  return [CrobagChangeEvent.eval(_cxt, this, "put", key, null, val)];
};
Crobag.prototype.put.nfargs = function() {
  return 1;
};
Crobag.prototype.upsert = function(_cxt, key, val) {
  return [CrobagChangeEvent.eval(_cxt, this, "upsert", key, null, val)];
};
Crobag.prototype.upsert.nfargs = function() {
  return 1;
};
Crobag.prototype.window = function(_cxt, from, size, handler) {
  return [CrobagWindowEvent.eval(_cxt, this, from, size, handler)];
};
Crobag.prototype.window.nfargs = function() {
  return 3;
};
Crobag.prototype.size = function(_cxt) {
  return this._entries.length;
};
Crobag.prototype.size.nfargs = function() {
  return 0;
};
Crobag.prototype._change = function(cx2, op, newKey, remove, val) {
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
Crobag.prototype._methods = function() {
  return {
    "insert": Crobag.prototype.insert,
    "put": Crobag.prototype.put,
    "size": Crobag.prototype.size,
    "upsert": Crobag.prototype.upsert,
    "window": Crobag.prototype.window
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
    arr.push(Send.eval(cx2, this.replyto, "next", [e.key, e.val], null));
  }
  arr.push(Send.eval(cx2, this.replyto, "done", [], _ActualSlideHandler.eval(cx2, this.crobag)));
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
var Image = function(_cxt, _uri) {
  FLObject.call(this, _cxt);
  this.state = _cxt.fields();
  this.state.set("uri", _uri);
};
Image._ctor_asset = function(_cxt, _card, _uri) {
  const ret2 = new Image(_cxt, _uri);
  return new ResponseWithMessages(_cxt, ret2, []);
};
Image._ctor_asset.nfargs = function() {
  return 2;
};
Image._ctor_uri = function(_cxt, _card, _uri) {
  const ret2 = new Image(_cxt, _uri);
  return new ResponseWithMessages(_cxt, ret2, []);
};
Image._ctor_uri.nfargs = function() {
  return 2;
};
Image.prototype.getUri = function() {
  var uri = this.state.get("uri");
  if (uri instanceof FLURI)
    uri = uri.resolve(window.location);
  return uri;
};
Image.prototype._compare = function(_cxt, other) {
  if (!(other instanceof Image))
    return false;
  return this.state.get("uri").toString() == other.state.get("uri").toString();
};
Image.prototype.toString = function() {
  return "Image " + this.state.get("uri");
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
      parent.style.height = ch + "px";
      img.style.height = ch + "px";
      parent.style.width = "auto";
      img.style.width = "auto";
      parent.style.top = "0px";
      var newImgWid = ch * imgrat;
      var left = -(newImgWid * xp / 100 - cw / 2);
      if (left + newImgWid < cw) {
        left = cw - newImgWid;
        if (left > 0)
          left /= 2;
      }
      parent.style.left = left + "px";
    } else {
      parent.style.width = cw + "px";
      img.style.width = cw + "px";
      parent.style.height = "auto";
      img.style.height = "auto";
      parent.style.left = "0px";
      var newImgHt = cw / imgrat;
      var top = -(newImgHt * yp / 100 - ch / 2);
      if (top + newImgHt < ch) {
        top = ch - newImgHt;
        if (top > 0)
          top /= 2;
      }
      parent.style.top = top + "px";
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
    img.style.width = xr + "px";
    img.style.height = yr + "px";
    img.style.left = xc - xr / 2 + "px";
    img.style.top = yc - yr / 2 + "px";
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
    img.style.width = dw + "px";
    img.style.height = dh + "px";
    if (sm > 0) {
      img.style.borderTopWidth = img.style.borderBottomWidth = md * sm + "px";
      img.style.borderLeftWidth = img.style.borderRightWidth = md * ar * sm + "px";
    }
  } else if (alg.startsWith("text-")) {
    var props = alg.replace("text-", "");
    var rs = parseFloat(props) / 100;
    var parent = img.parentElement;
    var ps = Math.min(parent.clientWidth, parent.clientHeight);
    var sz = rs * ps;
    parent.style.fontSize = sz + "px";
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
  var linkTitle;
  if (typeof value === "undefined" || value == null || !(value instanceof Link))
    linkRef = linkTitle = "";
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
  node.onclick = (ev) => env2.appl.relativeRoute(env2.newContext(), linkRef);
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
  if (value instanceof FLCard && value._destroyed) {
    value = null;
  }
  var div = document.getElementById(_renderTree._id);
  const node = div.querySelector("[data-flas-punnet='" + field + "']");
  if (!node.id) {
    var ncid = _cxt.nextDocumentId();
    node.id = ncid;
    _renderTree[field] = { _id: ncid, children: [] };
  }
  var crt = _renderTree[field];
  if (value instanceof FLError) {
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
    for (var i = 0; i < value.length; i++) {
      if (value[i]._destroyed) {
        value.splice(i, 1);
        --i;
      }
    }
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
    } else if (sw.op === "removefromend") {
      for (var i = value.length; i < crt.children.length; i++) {
        var child = crt.children[i];
        node.removeChild(child.value._containedIn);
      }
      crt.children.splice(value.length);
    } else if (sw.op === "disaster") {
      var rts = crt.children;
      var map = {};
      while (node.firstElementChild) {
        var nd = node.removeChild(node.firstElementChild);
        var rtc = rts.shift();
        map[nd.id] = { nd, rtc };
      }
      console.log("disaster map", sw.mapping, map);
      for (var i = 0; i < value.length; i++) {
        if (sw.mapping[i]) {
          var tmp = map[sw.mapping[i]];
          node.appendChild(tmp.nd);
          rts.push(tmp.rtc);
          delete map[sw.mapping[i]];
        } else {
          var e = value[i];
          var rt = { value: e };
          var inid = _cxt.nextDocumentId();
          rts.push(rt);
          const pe = document.createElement("div");
          pe.setAttribute("id", inid);
          node.appendChild(pe);
          e._renderInto(_cxt, pe);
          fn.call(this, _cxt, rt, node, null, e);
        }
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
  cx2.log("closing card", this.name());
  this._destroyed = true;
  cx2.unsubscribeAll(this);
};

// src/main/javascript/runtime/object.js
var FLObject = function(cx2) {
};
FLObject.prototype._updateTemplate = FLCard.prototype._updateTemplate;
FLObject.prototype._addItem = FLCard.prototype._addItem;
FLObject.prototype._updateContent = FLCard.prototype._updateContent;
FLObject.prototype._updateContainer = FLCard.prototype._updateContainer;
FLObject.prototype._updatePunnet = FLCard.prototype._updatePunnet;
FLObject.prototype._updateStyle = FLCard.prototype._updateStyle;
FLObject.prototype._updateList = FLCard.prototype._updateList;
FLObject.prototype._updateImage = FLCard.prototype._updateImage;
FLObject.prototype._updateLink = FLCard.prototype._updateLink;
FLObject.prototype._diffLists = FLCard.prototype._diffLists;
FLObject.prototype._attachHandlers = FLCard.prototype._attachHandlers;
FLObject.prototype._resizeDisplayElements = FLCard.prototype._resizeDisplayElements;

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
  FLObject.call(this, _cxt);
  this._card = _card;
  this.state = _cxt.fields();
  this.buffer = [];
};
Random._ctor_seed = function(_cxt, _card, s) {
  const ret2 = new Random(_cxt, _card);
  var seed = s ^ 3735928559;
  ret2.generateNext = xoshiro128(2654435769, 608135816, 3084996962, seed);
  return new ResponseWithMessages(_cxt, ret2, []);
};
Random._ctor_seed.nfargs = function() {
  return 2;
};
Random._ctor_unseeded = function(_cxt, _card) {
  const ret2 = new Random(_cxt, _card);
  var seed = Math.random() * 4294967295;
  ret2.generateNext = xoshiro128(2654435769, 608135816, 3084996962, seed);
  return new ResponseWithMessages(_cxt, ret2, []);
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
  return Send.eval(_cxt, this, "_used", [quant]);
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
  if (n instanceof FLError)
    return n;
  else if (typeof n !== "number")
    return new FLError("not a number");
  return new Interval(Math.floor(n / 86400), n % 86400 * 1e3 * 1e3 * 1e3);
};
FLBuiltin.seconds.nfargs = function() {
  return 1;
};
FLBuiltin.milliseconds = function(_cxt, n) {
  n = _cxt.full(n);
  if (n instanceof FLError)
    return n;
  else if (typeof n !== "number")
    return new FLError("not a number");
  return new Interval(Math.floor(n / 864e5), n % 864e5 * 1e3 * 1e3 * 1e3);
};
FLBuiltin.milliseconds.nfargs = function() {
  return 1;
};
FLBuiltin.fromunixdate = function(_cxt, n) {
  n = _cxt.full(n);
  if (n instanceof FLError)
    return n;
  else if (typeof n !== "number")
    return new FLError("not a number");
  return new Instant(Math.floor(n / 86400), n % 86400 * 1e3 * 1e3 * 1e3);
};
FLBuiltin.fromunixdate.nfargs = function() {
  return 1;
};
FLBuiltin.unixdate = function(_cxt, i) {
  i = _cxt.full(i);
  if (i instanceof FLError)
    return i;
  else if (!(i instanceof Instant))
    return new FLError("not an instant");
  var ds = i.days;
  var secs = i.ns / 1e3 / 1e3 / 1e3;
  return ds * 86400 + secs;
};
FLBuiltin.unixdate.nfargs = function() {
  return 1;
};
var Calendar = function(_cxt, _card) {
  FLObject.call(this, _cxt);
  this._card = _card;
  this.state = _cxt.fields();
};
Calendar._ctor_gregorian = function(_cxt, _card) {
  const ret2 = new Calendar(_cxt, _card);
  return new ResponseWithMessages(_cxt, ret2, []);
};
Calendar._ctor_gregorian.nfargs = function() {
  return 1;
};
Calendar.prototype.isoDateTime = function(_cxt, inst) {
  inst = _cxt.full(inst);
  if (inst instanceof FLError)
    return inst;
  else if (!(inst instanceof Instant))
    return new FLError("not an instant");
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
  if (n instanceof FLError)
    return n;
  else if (typeof n !== "string")
    return new FLError("not a string");
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
  this.structs["Link"] = Link;
  this.objects = {};
  this.objects["Random"] = Random;
  this.objects["FLBuiltin"] = FLBuiltin;
  this.objects["Crobag"] = Crobag;
  this.objects["CroEntry"] = CroEntry;
  this.objects["Html"] = Html;
  this.objects["Image"] = Image;
  this.objects["org.ziniki.common.ZiIdURI"] = ZiIdURI;
  this.objects["org.flasck.jvm.builtin.Crobag"] = Crobag;
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
    }, requestId: 0 };
};
CommonEnv.prototype.makeReady = function() {
  this.broker.register("Repeater", new ContainerRepeater());
};
CommonEnv.prototype.clear = function() {
  document.body.innerHTML = "";
  this.cards = [];
  this.nextDivId = 1;
  this.divSince = this.nextDivId;
  this.namedSubscriptions = /* @__PURE__ */ new Map();
  this.unnamedSubscriptions = /* @__PURE__ */ new Map();
  this.singletons = {};
};
CommonEnv.prototype.queueMessages = function(_cxt, msg) {
  var reqId = this.locker.requestId++;
  this.locker.lock(reqId, "queue");
  this.queue.push(msg);
  var self = this;
  setTimeout(() => {
    try {
      self.dispatchMessages(_cxt);
    } catch (e) {
      self.logger.log(e);
    } finally {
      this.locker.unlock(reqId, "queue");
    }
  }, 0);
};
CommonEnv.prototype.quiescent = function() {
  return this.queue.length == 0;
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
  else if (msg instanceof FLError || typeof msg == "string") {
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

// src/main/javascript/runtime/cstore.js
import { NamedIdempotentHandler as NamedIdempotentHandler2, proxy } from "/js/ziwsh.js";
var ContractStore = function(_cxt) {
  this.env = _cxt.env;
  this.recorded = {};
  this.toRequire = {};
};
ContractStore.prototype.record = function(_cxt, name, impl) {
  this.recorded[name] = impl;
  _cxt.broker.register(name, impl);
};
ContractStore.prototype.contractFor = function(_cxt, name) {
  const ret2 = this.recorded[name];
  if (!ret2)
    throw new Error("There is no contract for " + name);
  return ret2;
};
ContractStore.prototype.require = function(_cxt, name, clz) {
  const ctr = _cxt.broker.contracts[clz];
  const di = new DispatcherInvoker(this.env, _cxt.broker.require(clz));
  const px = proxy(_cxt, ctr, di);
  px._areYouA = function(cx2, ty) {
    return ty === clz;
  };
  this.toRequire[name] = px;
};
ContractStore.prototype.required = function(_cxt, name) {
  const ret2 = this.toRequire[name];
  if (!ret2)
    throw new Error("There is no provided contract for var " + name);
  return ret2;
};
var DispatcherInvoker = function(env2, call) {
  this.env = env2;
  this.call = call;
};
DispatcherInvoker.prototype.invoke = function(meth, args) {
  var pass = args.slice(1, args.length - 1);
  var hdlr = args[args.length - 1];
  var hdlrName = null;
  if (hdlr instanceof NamedIdempotentHandler2) {
    hdlrName = hdlr._name;
    hdlr = hdlr._handler;
  }
  var cx2 = args[0];
  if (!cx2.subcontext) {
    cx2 = cx2.bindTo(hdlr);
  }
  var resp = Send.eval(cx2, this.call, meth, pass, hdlr, hdlrName);
  this.env.queueMessages(cx2, resp);
};

// src/main/javascript/runtime/entity.js
var Entity = function() {
};
Entity.prototype._field_id = function(cx2, args) {
  return this.state.get("_id");
};
Entity.prototype._field_id.nfargs = function() {
  return 0;
};
export {
  Application,
  Assign,
  AssignCons,
  AssignItem,
  Calendar,
  ClickEvent,
  CommonEnv,
  Cons,
  ContractStore,
  CroEntry,
  Crobag,
  CrobagChangeEvent,
  CrobagWindow,
  CrobagWindowEvent,
  Debug,
  Entity,
  FLBuiltin,
  FLCard,
  FLContext,
  FLError,
  FLObject,
  FLURI,
  False,
  HashPair,
  Image,
  Instant,
  Interval,
  Link,
  MakeHash,
  Nil,
  Random,
  ResponseWithMessages,
  ScrollTo,
  Send,
  SlideWindow,
  True,
  Tuple,
  TypeOf,
  UpdateDisplay
};

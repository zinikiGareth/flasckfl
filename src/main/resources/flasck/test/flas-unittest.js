
const UTRunner = function(bridge) {
	if (!bridge)
		bridge = console; // at least get the logger ...
	CommonEnv.call(this, bridge, new SimpleBroker(bridge, this, {}));
	this.errors = [];
	this.mocks = {};
	this.ajaxen = [];
	this.appls = [];
	this.activeSubscribers = [];
	if (typeof(window) !== 'undefined')
		window.utrunner = this;
	this.moduleInstances = {};
	this.toCancel = new Map();
	for (var mn in UTRunner.modules) {
		if (UTRunner.modules.hasOwnProperty(mn)) {
			var jm;
			if (bridge.module) {
				jm = bridge.module(this, mn);
				if (jm == 'must-wait')
					continue;
			}
			this.moduleInstances[mn] = new UTRunner.modules[mn](this, jm);
		}
	}
}

UTRunner.prototype = new CommonEnv();
UTRunner.prototype.constructor = UTRunner;

UTRunner.modules = {};

UTRunner.prototype.bindModule = function(name, jm) {
	this.moduleInstances[name] = new UTRunner.modules[name](this, jm);
}

UTRunner.prototype.makeReady = function() {
	CommonEnv.prototype.makeReady.call(this);
    this.broker.register("Ajax", new MockAjaxService());
}

UTRunner.prototype.error = function(err) {
	this.errors.push(err);
}
UTRunner.prototype.handleMessages = function(_cxt, msg) {
	return CommonEnv.prototype.handleMessages.call(this, _cxt, msg);
}
UTRunner.prototype.assertSameValue = function(_cxt, e, a) {
	e = _cxt.full(e);
	if (e instanceof ResponseWithMessages)
		e = e.obj; // just throw the messages away
	a = _cxt.full(a);
	if (!_cxt.compare(e, a)) {
		if (a instanceof FLError)
			a = a.message;
		throw new Error("NSV\n  expected: " + e + "\n  actual:   " + a);
	}
}
UTRunner.prototype.assertIdentical = function(_cxt, e, a) {
	e = _cxt.full(e);
//	if (e instanceof ResponseWithMessages)
//		e = e.obj; // just throw the messages away
	a = _cxt.full(a);
	if (a !== e) {
		if (a instanceof FLError)
			a = a.message;
		throw new Error("NSV\n  expected: " + e + "\n  actual:   " + a);
	}
}
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
		// we don't have a lot of choice but to update all cards
		this.updateAllCards(_cxt);
	}
}
UTRunner.prototype.close = function(_cxt, dest) {
	dest = _cxt.full(dest);
	if (dest instanceof MockCard) {
		dest = dest.card;
	}
	dest._close(_cxt);
	// we don't have a lot of choice but to update all cards
	this.updateAllCards(_cxt);
}
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
}
UTRunner.prototype.send = function(_cxt, target, contract, msg, inargs) {
	_cxt.log("doing send from runner to " + contract + ":" + msg);
	var reply;
	var args = [];
	for (var i=0;i<inargs.length;i++) {
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
}
UTRunner.prototype.render = function(_cxt, target, fn, template) {
	var sendTo = this.findMockFor(target);
	if (!sendTo)
		throw Error("there is no mock " + target);
	sendTo.rt = {};
	if (sendTo.div) {
		sendTo.div.innerHTML = '';
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
	}
	sendTo.redraw(_cxt);
}
UTRunner.prototype.findMockFor = function(obj) {
	if (obj instanceof MockFLObject || obj instanceof MockCard || obj instanceof MockAppl)
		return obj;
	var ks = Object.keys(this.mocks);
	for (var i=0;i<ks.length;i++) {
		if (this.mocks[ks[i]].obj == obj)
			return this.mocks[ks[i]];
	}
	throw new Error("no mock for " + obj);
}
UTRunner.prototype.event = function(_cxt, target, zone, event) {
	var sendTo = this.findMockFor(target);
	if (!sendTo)
		throw Error("there is no mock " + target);
	var div = null;
	var receiver;
	if (sendTo instanceof MockCard)
		receiver = sendTo.card;
	else if (sendTo instanceof MockFLObject)
		receiver = sendTo; // presuming an object
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
}
UTRunner.prototype.input = function(_cxt, target, zone, text) {
	var sendTo = this.findMockFor(target);
	if (!sendTo)
		throw Error("there is no mock " + target);
	var receiver;
	if (sendTo instanceof MockCard)
		receiver = sendTo.card;
	else if (sendTo instanceof MockFLObject)
		receiver = sendTo; // presuming an object
	else
		throw Error("cannot send event to " + target);
	var div = this.findDiv(_cxt, receiver._currentRenderTree(), zone, 0);
	if (div) {
		text = _cxt.full(text);
		if (text instanceof Error) {
			_cxt.log(text);
			return;
		}
		if (!div.tagName == "INPUT" || !div.hasAttribute("type") || (div.getAttribute("type") != "text" && div.getAttribute("type") != "password")) {
			_cxt.log("can only set input text on input elements of type text or password");
			return;
		}
		div.setAttribute("value", text);
	}
}
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
		return this.findDiv(_cxt, rt.children[first[1]], zone, pos+1);
	} else {
		const inner = this._findSubThroughSingle(rt, first[1]);
		if (!inner) {
			throw Error("MATCH\nThere is no element " + first[1] + " in " + this._nameOf(zone, pos));
		}
		return this.findDiv(_cxt, inner, zone, pos+1);
	}
}
UTRunner.prototype._findSubThroughSingle = function(rt, name) {
	while (true) {
		var ret = rt[name];
		if (ret)
			return ret;
		rt = rt['single'];
		if (rt == null)
			return null;
	}
}
UTRunner.prototype._nameOf = function(zone, pos) {
	if (pos == 0)
		return "card";
	var ret = "";
	for (var i=0;i<pos;i++) {
		if (ret)
			ret += '.';
		ret += zone[i][1];
	}
	return ret;
}
UTRunner.prototype.getZoneDiv = function(_cxt, target, zone) {
	if (!target || !target._currentRenderTree) {
		throw Error("MATCH\nThe card has no rendered content");
	}
	// will throw error if not found
	return this.findDiv(_cxt, target._currentRenderTree(), zone, 0);
}
UTRunner.prototype.matchText = function(_cxt, target, zone, contains, fails, expected) {
	var matchOn = this.findMockFor(target);
	if (!matchOn)
		throw Error("there is no mock " + target);
	try {
		var div = this.getZoneDiv(_cxt, matchOn, zone);
	} catch (e) {
		if (fails)
			return; // we were expecting that ...
		else
			throw e;
	}
	var actual = div.innerText.trim();
	actual = actual.replace(/\n/g, ' ');
	actual = actual.replace(/ +/, ' ');
	actual = actual.trim();
	if (contains) {
		if (!actual.includes(expected))
			throw new Error("MATCH\n  expected to contain: " + expected + "\n  actual:   " + actual);
	} else {
		if (actual != expected)
			throw new Error("MATCH\n  expected: " + expected + "\n  actual:   " + actual);
	}
}
UTRunner.prototype.matchTitle = function(_cxt, target, zone, contains, expected) {
	var matchOn = this.findMockFor(target);
	if (!matchOn)
		throw Error("there is no mock " + target);
	if (!(matchOn instanceof MockAppl))
		throw Error("can only test title on Appl");
	var titles = document.head.getElementsByTagName("title");
	var actual = "";
	for (var i=0;i<titles.length;i++) {
		actual += titles[i].innerText.trim() + " ";
	}
	actual = actual.trim();
	actual = actual.replace(/\n/g, ' ');
	actual = actual.replace(/ +/, ' ');
	if (contains) {
		if (!actual.includes(expected))
			throw new Error("MATCH\n  expected to contain: " + expected + "\n  actual:   " + actual);
	} else {
		if (actual != expected)
			throw new Error("MATCH\n  expected: " + expected + "\n  actual:   " + actual);
	}
}
UTRunner.prototype.matchImageUri = function(_cxt, target, zone, expected) {
	var matchOn = this.findMockFor(target);
	if (!matchOn)
		throw Error("there is no mock " + target);
	var div = this.getZoneDiv(_cxt, matchOn, zone);
	if (div.tagName != "IMG")
		throw new Error("MATCH\n  expected: IMG\n  actual:   " + div.tagName);

	var abs = new URL(expected, window.location).toString()
	var actual = div.src;
	if (actual != abs)
		throw new Error("MATCH\n  expected: " + abs + "\n  actual:   " + actual);
}
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
}
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
	for (var i=0;i<explist.length;i++) {
		var exp = explist[i];
		failed |= !clzlist.includes(exp);
	}
	if (!contains)
		failed |= clzlist.length != explist.length;
	if (failed) {
		if (contains)
			throw new Error("MATCH\n  expected to contain: " + explist.join(' ') + "\n  actual: " + clzlist.join(' '));
		else
			throw new Error("MATCH\n  expected: " + explist.join(' ') + "\n  actual:   " + clzlist.join(' '));
	}
}
UTRunner.prototype.matchScroll = function(_cxt, target, zone, contains, expected) {
	var matchOn = this.findMockFor(target);
	if (!matchOn)
		throw Error("there is no mock " + target);
	var div = this.getZoneDiv(_cxt, matchOn, zone);
	var actual = div.scrollTop;
	if (actual != expected)
		throw new Error("MATCH\n  expected: " + expected + "\n  actual:   " + actual);
}
UTRunner.prototype.route = function(_cxt, app, route, storeCards) {
	app.route(_cxt, route, () => {
		app.bindCards(_cxt, storeCards);
	});
}
UTRunner.prototype.userlogin = function(_cxt, app, user) {
	app.userLoggedIn(_cxt, user);
}
UTRunner.prototype.updateCard = function(_cxt, card) {
	if (!(card instanceof MockCard))
		return;
	if (card.card._updateDisplay)
		card.card._updateDisplay(_cxt, card.card._renderTree);
}
UTRunner.prototype.checkAtEnd = function() {
	if (this.errors.length > 0)
		throw this.errors[0];
}
UTRunner.prototype.newdiv = function(cnt) {
	if (cnt != null) { // specifically null, because we want to check on 0
		if (cnt != this.nextDivId - this.divSince) {
			throw Error("NEWDIV\n  expected: " + cnt + "\n  actual:   " + (this.nextDivId - this.divSince));
		}
	}
	this.divSince = this.nextDivId;
}
UTRunner.prototype.expectCancel = function(handler) {
	var hn;
	if (handler instanceof NamedIdempotentHandler) {
		hn = handler._ihid;
	} else {
		throw new Error("not handled");
	}
	this.toCancel.set(hn, handler);
}
UTRunner.prototype.cancelBound = function(varName, handlerName) {
	if (!this.toCancel.has(handlerName)) {
		throw new Error("UECAN\n  cancelled " + varName + " but it was not expected");
	}
	this.toCancel.delete(handlerName);
}
UTRunner.prototype.assertSatisfied = function() {
	if (this.toCancel.size != 0) {
		throw new Error("EXPCAN\n  subscription " /* + this.toCancel[0]*/ + "was not cancelled");
	}
}
UTRunner.prototype.mockAgent = function(_cxt, agent) {
	return new MockAgent(agent);
}
UTRunner.prototype.mockCard = function(_cxt, name, card) {
	var ret = new MockCard(_cxt, card);
	this.mocks[name] = ret;
	this.cards.push(ret);
	return ret;
}
UTRunner.prototype.newAjax = function(cxt, baseUri) {
	var ma = new MockAjax(cxt, baseUri);
	this.ajaxen.push(ma);
	return ma;
}
UTRunner.prototype.newMockAppl = function(cxt, clz) {
	var ma = new MockAppl(cxt, clz);
	this.appls.push(ma);
	return ma;
}
UTRunner.prototype._updateDisplay = function(_cxt, rt) {
	this.updateAllCards(_cxt);
}
UTRunner.prototype.updateAllCards = function(_cxt) {
	for (var i=0;i<this.cards.length;i++) {
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
}
UTRunner.prototype.module = function(mod) {
	var m = this.moduleInstances[mod];
	if (!m)
		throw new Error("There is no module " + mod);
	return m;
}
UTRunner.prototype.transport = function(tz) {
	// we have a transport to Ziniki
	this.zinBch = new JsonBeachhead(this, "fred", this.broker, tz);
	this.broker.beachhead(this.zinBch);
}
UTRunner.prototype.deliver = function(json) {
	// we have a response from Ziniki
	this.logger.log("have " + json + " ready for delivery");
	var cx = this.newContext();
	var msgs = this.zinBch.dispatch(cx, json, null);
	this.logger.log("have messages", msgs);
	this.queueMessages(cx, msgs);
}
UTRunner.prototype.addHistory = function(state, title, url) {
	// we could forward this to the bridge if we wanted to do something specific
}

UTRunner.prototype.runRemote = function(testClz, spec) {
	var cxt = this.newContext();
	var st = new testClz(this, cxt);
	var allSteps = [];
	if (spec.configure) {
		var steps = spec.configure.call(st, cxt);
		for (var j=0;j<steps.length;j++)
			allSteps.push(steps[j]);
	}
	if (spec.stages) {
		for (var i=0;i<spec.stages.length;i++) {
			var steps = spec.stages[i].call(st, cxt);
			for (var j=0;j<steps.length;j++)
				allSteps.push(steps[j]);
		}
	}
	if (spec.cleanup) {
		var steps = spec.cleanup.call(st, cxt);
		for (var j=0;j<steps.length;j++)
			allSteps.push(steps[j]);
	}
	var bridge = this.logger; // we have stored it as "logger" but it is actually the bridge to "Java-world"
	bridge.executeSync(this, st, cxt, allSteps);
}

const makeBridge = function(jsb, logger) {
	return {
		log: logger.log,
		debugmsg: logger.debugmsg,
		// sendJson: (j) => jsb.sendJson.call(jsb, j),
		// transport: (z) => jsb.transport.call(jsb, z),
		module: (r, m) => jsb.module.call(jsb, r, m),
		error: (e) => jsb.error.call(jsb, e),
		lock: () => jsb.lock.call(jsb),
		unlock: () => jsb.unlock.call(jsb),
		getTestCounter: () => jsb.getTestCounter.call(jsb)
	};
}

{
	window.UTRunner = UTRunner;
	window.makeBridge = makeBridge;
}
function STSecurityModule() {
	this.currentUser = null;
}

STSecurityModule.prototype.requireLogin = function() {
	return this.currentUser != null;
}

STSecurityModule.prototype.userLoggedIn = function(_cxt, app, user) {
	this.currentUser = user;
	app.nowLoggedIn(_cxt);
}

const BoundVar = function(name) {
	this.name = name;
}

BoundVar.prototype.bindActual = function(obj) {
	if (this.actual) {
		throw Error("cannot rebind bound var");
	}
	this.actual = obj;
}
BoundVar.prototype.introduced = function() {
	if (!this.actual)
		throw Error("bound var has not yet been bound");
	return this.actual;
}

const Expectation = function(args, handler) {
	this.args = args;
	this.handler = handler;
	this.allowed = 1;
	this.invoked = 0;
}

Expectation.prototype.allow = function(n) {
	this.allowed = n;
}

const proxyMe = function(self, meth) {
	return function(cx, ...rest) {
		self.serviceMethod(cx, meth, rest);
	}
}

const MockContract = function(ctr) {
	this.ctr = ctr;
	this.expected = {};
	this.methodNames = {};
	var ms = ctr._methods();
	for (var i in ms) {
		this.methodNames[ms[i]] = this[ms[i]] = proxyMe(this, ms[i]);
	}
	this._methods = function() {
		return this.methodNames;
	}
};

MockContract.prototype._areYouA = function(cx, ty) {
	return this.ctr.name() == ty;
}

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
}

MockContract.prototype.serviceMethod = function(_cxt, meth, args) {
	if (meth === 'success' || meth === 'failure')
		return; // these should really just be part of the protocol
	const ih = args[args.length-1];
	args = args.slice(0, args.length-1);
	if (!this.expected[meth]) {
		_cxt.env.error(new Error("EXP\n  There are no expectations on " + this.ctr.name() + " for " + meth));
		return;
	}
	const exp = this.expected[meth];
	var pending = null;
	for (var i=0;i<exp.length;i++) {
		// TOOD: should see if exp[i].args[j] is a BoundVar
		// I think this would involve us unwrapping this "list compare" and comparing each argument one at a time
		// wait for it to come up though
		if (_cxt.compare(exp[i].args, args)) {
			var matched = exp[i];
			if (matched.invoked == matched.allowed) {
				pending = new Error("EXP\n  " + this.ctr.name() + "." + meth + " " + args + " already invoked (allowed=" + matched.allowed +"; actual=" + matched.invoked +")");
				continue; // there may be another that matches
			}
			matched.invoked++;
			_cxt.log("Have invocation of", meth, "with", args);
			if (matched.handler instanceof BoundVar) {
				var tih = ih;
				// if (ih instanceof NamedIdempotentHandler) {
				// 	tih = ih._handler;
				// }
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
}

MockContract.prototype.assertSatisfied = function(_cxt) {
	var msg = "";
	for (var meth in this.expected) {
		if (!this.expected.hasOwnProperty(meth))
			continue;
		var exp = this.expected[meth];
		for (var i=0;i<exp.length;i++) {
			if (exp[i].invoked != exp[i].allowed)
				msg += "  " + this.ctr.name() + "." + meth + " <" + i +">\n";
		}
	}
	if (msg)
		throw new Error("UNUSED\n" + msg);
}

const SubscriptionFor = function(varName, handlerName) {
	this.varName = varName;
	this.handlerName = handlerName;
};

SubscriptionFor.prototype.cancel = function(cx) {
	cx.env.cancelBound(this.varName, this.handlerName);
}

const MockFLObject = function(obj) {
	this.obj = obj;
}

MockFLObject.prototype._currentDiv = function() {
	if (this.div)
		return this.div;
	else
		return null;
}

MockFLObject.prototype._currentRenderTree = function() {
	if (this.rt)
		return this.rt.result.single;
	else
		return null;
}

const MockAgent = function(agent) {
	this.agent = agent;
};

MockAgent.prototype.sendTo = function(_cxt, contract, msg, args) {
	const ctr = this.agent._contracts.contractFor(_cxt, contract);
	const inv = Array.from(args);
	inv.splice(0, 0, _cxt);
	return ctr[msg].apply(ctr, inv);
};

const MockCard = function(cx, card) {
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
}

MockCard.prototype._currentRenderTree = function() {
	return this.card._renderTree;
}

MockCard.prototype._underlying = function(_cxt) {
	return this.card;
}

const ExplodingIdempotentHandler = function(cx) {
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
	for (var i=0;i<this.failures.length;i++) {
		const f = this.failures[i];
		if (f.msg === msg) {
			f.actual++;
			return;
		}
	}
	this.failures.push({msg, expected: 0, actual: 1});
};

ExplodingIdempotentHandler.prototype.expectFailure = function(msg) {
	this.cx.log("expect failure: " + msg);
	this.failures.push({msg, expected: 1, actual: 0});
};

ExplodingIdempotentHandler.prototype.assertSatisfied = function() {
	var msg = "";
    for (var i=0;i<this.failures.length;i++) {
		const f = this.failures[i];
		if (f.expected === 0) {
			msg += "  failure: unexpected IH failure: " + f.msg;
		} else if (f.expected != f.actual) {
			msg += "  failure: " + f.msg + " (expected: " + f.expected + ", actual: " + f.actual +")\n";
		}
	}
	if (msg)
		throw new Error("HANDLERS\n" + msg);
};

const MockHandler = function(ctr) {
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
	}
};

MockHandler.prototype = new ExplodingIdempotentHandler();
MockHandler.prototype.constructor = MockHandler;

MockHandler.prototype._areYouA = MockContract.prototype._areYouA;
MockHandler.prototype.expect = MockContract.prototype.expect;
MockHandler.prototype.serviceMethod = MockContract.prototype.serviceMethod;
MockHandler.prototype.assertSatisfied = MockContract.prototype.assertSatisfied;

const MockAjax = function(_cxt, baseUri) {
	this.baseUri = baseUri;
	this.expect = { subscribe: [] }
}
MockAjax.prototype.expectSubscribe = function(_cxt, path) {
	var mas = new MockAjaxSubscriber(_cxt, path);
	this.expect.subscribe.push(mas);
	return mas;
}
MockAjax.prototype.pump = function(_cxt) {
	for (var i=0;i<this.expect.subscribe.length;i++) {
		this.expect.subscribe[i].dispatch(_cxt, this.baseUri, _cxt.env.activeSubscribers);
	}
}

const MockAjaxSubscriber = function(_cxt, path) {
	this.path = path;
	this.responses = [];
	this.nextResponse = 0;
}
MockAjaxSubscriber.prototype.response = function(_cxt, val) {
	this.responses.push(val);
}
MockAjaxSubscriber.prototype.dispatch = function(_cxt, baseUri, subscribers) {
	if (this.nextResponse >= this.responses.length)
		return;
	for (var i=0;i<subscribers.length;i++) {
		if (this.matchAndSend(_cxt, baseUri, subscribers[i]))
			return;
	}
	// no message - is this an error or just one of those things?
}
MockAjaxSubscriber.prototype.matchAndSend = function(_cxt, baseUri, sub) {
	if (sub.uri.toString() == new URL(this.path, baseUri).toString()) {
		var resp = this.responses[this.nextResponse++];
		resp = _cxt.full(resp);
		if (resp instanceof FLError) {
			// I think we need to report it and fail the test
			_cxt.log(resp);
			return true;
		}
		var msg;
		if (resp instanceof AjaxMessage) {
			msg = resp;
		} else {
			msg = new AjaxMessage(_cxt);
			msg.state.set('headers', []);
			if (typeof(resp) === "string")
				msg.state.set('body', resp);
			else
				msg.state.set('body', JSON.stringify(resp));
		}
		_cxt.env.queueMessages(_cxt, Send.eval(_cxt, sub.handler, "message", [msg], null));
		_cxt.env.dispatchMessages(_cxt);
		return true;
	} else
		return false;
}

// The service that attempts to connect ...
const MockAjaxService = function() {
}
MockAjaxService.prototype.subscribe = function(_cxt, uri, options, handler) {
	if (uri instanceof FLURI)
		uri = uri.uri;
	_cxt.env.activeSubscribers.push({ uri, options, handler });
}

const MockAppl = function(_cxt, clz) {
	const newdiv = document.createElement("div");
	newdiv.setAttribute("id", _cxt.nextDocumentId());
	document.body.appendChild(newdiv);
	this.appl = new clz._Application(_cxt, newdiv);
	this.appl._updateDisplay(_cxt, this.appl._currentRenderTree());
}
MockAppl.prototype.route = function(_cxt, r, andThen) {
	this.appl.gotoRoute(_cxt, r, () => {
		this.appl._updateDisplay(_cxt, this.appl._currentRenderTree());
		andThen();
	});
}
MockAppl.prototype.userLoggedIn = function(_cxt, u) {
	this.appl.securityModule.userLoggedIn(_cxt, this.appl, u);
}
MockAppl.prototype.bindCards = function(_cxt, iv) {
	if (!iv)
		return;
	var binding = {};
	binding["main"] = this.appl.cards["main"];
	iv.bindActual({ routes: binding });
}
MockAppl.prototype._currentRenderTree = function() {
	return this.appl._currentRenderTree();
}

// Connect to ChromeTestRunner
function WSBridge(host, port) {
	var self = this;
	this.ws = new WebSocket("ws://" + host + ":" + port + "/bridge");
	this.waitcount = 1;
	this.requestId = 1;
	this.sending = [];
	this.lockedOut = [];
	this.responders = {};
	this.ws.addEventListener("open", ev => {
		console.log("connected", ev);
		while (this.sending.length > 0) {
			var v = this.sending.shift();
			this.ws.send(v);
		}
	});
	this.ws.addEventListener("message", ev => {
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
}

WSBridge.prototype.debugmsg = function(...args) {
	console.log.apply(console.log, args);
}

WSBridge.prototype.module = function(runner, moduleName) {
	this.runner = runner;
	this.send({action: "module", "name": moduleName });
	this.lock("bindModule");
	return 'must-wait';
}

WSBridge.handlers['haveModule'] = function(msg) {
	var name = msg.name;
	var clz = window[msg.clz];
	var conn = msg.conn;

	console.log("have connection for module", this, name, clz);
	this.runner.bindModule(name, new clz(this, conn));
	this.unlock("haveModule");
}

WSBridge.prototype.send = function(json) {
	var text = JSON.stringify(json);
	if (this.ws.readyState == this.ws.OPEN)
		this.ws.send(text);
	else
		this.sending.push(text)
}

WSBridge.prototype.connectToZiniki = function(wsapi, cb) {
	runner.broker.connectToServer('ws://' + host + ':' + port);
}

WSBridge.prototype.executeSync = function(runner, st, cxt, steps) {
	this.runner = runner;
	this.st = st;
	this.runcxt = cxt;
	this.readysteps = steps;
	this.unlock("ready to go"); // unlocks the initial "1" we set in constructor
}

WSBridge.prototype.nextRequestId = function(hdlr) {
	this.responders[this.requestId] = hdlr;
	return this.requestId++;
}

WSBridge.prototype.lock = function(msg) {
	this.waitcount++;
	console.log("lock   waitcount = " + this.waitcount, msg);
}

WSBridge.prototype.unlock = function(msg) {
	--this.waitcount;
	console.log("unlock waitcount = " + this.waitcount, msg);
	if (this.waitcount == 0) {
		console.log(new Date() + " ready to go");
		this.gotime();
	}
}

WSBridge.prototype.onUnlock = function(f) {
	this.lockedOut.push(f);
}

WSBridge.prototype.gotime = function() {
	if (this.readysteps.length == 0) {
		// we're done
		console.log("test complete");
		return;
	}
	if (this.waitcount > 0) {
		console.log("cannot go because lock count is", this.waitcount);
		return; // we are in a holding pattern
	}
	if (this.lockedOut.length  > 0) {
		console.log("handling locked out callback");
		this.lock("a callback");
		this.lockedOut.shift().call(this);
		return;
	}
	setTimeout(() => {
		if (this.readysteps.length == 0) {
			// we're done
			console.log("no more steps");
			return;
		}
		var s = this.readysteps.shift();
		console.log(new Date() + " executing step", s);
		this.lock("around step");
		this.st[s].call(this.st, this.runcxt);
		this.send({action: "step"});
	}, 100);
}

WSBridge.handlers["stepdone"] = function(msg) {
	this.unlock("around step");
}

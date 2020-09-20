
const UTRunner = function(logger) {
	if (!logger)
		logger = console.log;
	CommonEnv.call(this, logger, new SimpleBroker(logger, this, {}));
	this.errors = [];
	this.mocks = {};
}

UTRunner.prototype = new CommonEnv();
UTRunner.prototype.constructor = UTRunner;

UTRunner.prototype.error = function(err) {
	this.errors.push(err);
}
UTRunner.prototype.handleMessages = function(_cxt, msg) {
	if (this.errors.length != 0)
		throw this.errors[0];
	return CommonEnv.prototype.handleMessages.call(this, _cxt, msg);
}
UTRunner.prototype.assertSameValue = function(_cxt, e, a) {
	e = _cxt.full(e);
	a = _cxt.full(a);
	if (!_cxt.compare(e, a)) {
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
UTRunner.prototype.invoke = function(_cxt, inv) {
	inv = _cxt.full(inv);
	this.queueMessages(_cxt, inv);
	this.dispatchMessages(_cxt);
}
UTRunner.prototype.send = function(_cxt, target, contract, msg, args) {
	var reply = target.sendTo(_cxt, contract, msg, args);
	reply = _cxt.full(reply);
	this.queueMessages(_cxt, reply);
	this.dispatchMessages(_cxt);
	this.updateCard(_cxt, target);
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
	if (obj instanceof MockFLObject || obj instanceof MockCard)
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
		div.dispatchEvent(event._makeJSEvent(_cxt));
		this.dispatchMessages(_cxt);
	}
}
UTRunner.prototype.findDiv = function(_cxt, rt, zone, pos) {
	if (pos >= zone.length) {
		return document.getElementById(rt._id);
	}
	const first = zone[pos];
	if (first[0] == "item") {
		if (!rt.children || first[1] >= rt.children.length) {
			throw Error("MATCH\nThere is no child " + first[1] + " in " + this._nameOf(zone, pos));
		}
		return this.findDiv(_cxt, rt.children[first[1]], zone, pos+1);
	} else {
		if (!rt[first[1]]) {
			throw Error("MATCH\nThere is no element " + first[1] + " in " + this._nameOf(zone, pos));
		}
		return this.findDiv(_cxt, rt[first[1]], zone, pos+1);
	}
}
UTRunner.prototype._nameOf = function(zone, pos) {
	if (pos == 0)
		return "card";
	var ret = "";
	for (var i=0;i<pos;i++) {
		if (ret)
			ret += '.';
		ret += zone[pos][1];
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
UTRunner.prototype.matchText = function(_cxt, target, zone, contains, expected) {
	var matchOn = this.findMockFor(target);
	if (!matchOn)
		throw Error("there is no mock " + target);
	var div = this.getZoneDiv(_cxt, matchOn, zone);
	var actual = div.innerText.trim();
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
UTRunner.prototype.mockAgent = function(_cxt, agent) {
	return new MockAgent(agent);
}
UTRunner.prototype.mockCard = function(_cxt, name, card) {
	var ret = new MockCard(_cxt, card);
	this.mocks[name] = ret;
	this.cards.push(ret);
	return ret;
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

	window.UTRunner = UTRunner;


const BoundVar = function() {
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
	const ih = args[args.length-1];
	args = args.slice(0, args.length-1);
	if (!this.expected[meth]) {
		_cxt.env.error(new Error("There are no expectations on " + this.ctr.name() + " for " + meth));
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
				pending = new Error(this.ctr.name() + "." + meth + " " + args + " already invoked (allowed=" + matched.allowed +"; actual=" + matched.invoked +")");
				continue; // there may be another that matches
			}
			matched.invoked++;
			_cxt.log("Have invocation of", meth, "with", args);
			if (matched.handler instanceof BoundVar) {
				matched.handler.bindActual(ih);
			}
			return;
		}
	}
	if (pending) {
		_cxt.env.error(pending);
		return;
	} else {
		_cxt.env.error(new Error("Unexpected invocation: " + this.ctr.name() + "." + meth + " " + args));
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

const MockFLObject = function(obj) {
	this.obj = obj;
}

MockFLObject.prototype._currentDiv = function() {
	if (this.div)
		return this.div;
	else
		throw Error("You must render the object first");
}

MockFLObject.prototype._currentRenderTree = function() {
	if (this.rt)
		return this.rt.result.single;
	else
		throw Error("You must render the object first");
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



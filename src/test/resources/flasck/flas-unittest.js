
const UTRunner = {};
UTRunner.assertSameValue = function(_cxt, e, a) {
	e = _cxt.full(e);
	a = _cxt.full(a);
	if (!_cxt.compare(e, a)) {
		throw new Error("NSV\n  expected: " + e + "\n  actual:   " + a);
	}
}
UTRunner.invoke = function(_cxt, inv) {
	inv = _cxt.full(inv);
	handleMessages(_cxt, inv);
}
UTRunner.send = function(_cxt, target, contract, msg, args) {
	var reply = target.sendTo(_cxt, contract, msg, args);
	reply = _cxt.full(reply);
	handleMessages(_cxt, reply);
}
const handleMessages = function(_cxt, msg) {
	if (!msg || msg instanceof FLError)
		return;
	else if (msg instanceof Array) {
		for (var i=0;i<msg.length;i++) {
			handleMessages(_cxt, msg[i]);
		}
	} else if (msg) {
		var ret = msg.dispatch(_cxt);
		if (ret)
			handleMessages(_cxt, ret);
	}
}
UTRunner.newContext = function(logger) {
	if (logger) {
		this.logger = logger;
	}
	return new FLContext(this);
}

	window.runner = UTRunner;

const Expectation = function(args) {
	this.args = args;
	this.allowed = 1;
	this.invoked = 0;
}

Expectation.prototype.allow = function(n) {
	this.allowed = n;
}

const MockContract = function(ctr) {
	this.ctr = ctr;
	this.expected = {};
};

MockContract.prototype.areYouA = function(ty) {
	return this.ctr.name() == ty;
}

MockContract.prototype.expect = function(meth, args) {
	if (!this.expected[meth])
		this.expected[meth] = [];
	if (!this.ctr[meth] || !this.ctr[meth].nfargs) {
		throw new Error("EXP\n  " + this.ctr.name() + " does not have a method " + meth);
	}
	const expArgs = this.ctr[meth].nfargs();
	if (args.length != expArgs) {
		throw new Error("EXP\n  " + this.ctr.name() + "." + meth + " expects " + expArgs + " parameters, not " + args.length);
	}

	const exp = new Expectation(args);
	this.expected[meth].push(exp);
	return exp;
}

MockContract.prototype.serviceMethod = function(_cxt, meth, args) {
	if (!this.expected[meth])
		throw new Error("EXP\n  There are no expectations on " + this.ctr.name() + " for " + meth);
	const exp = this.expected[meth];
	var matched = null;
	for (var i=0;i<exp.length;i++) {
		if (_cxt.compare(exp[i].args, args)) {
			matched = exp[i];
			break;
		}
	}
	if (!matched) {
		throw new Error("EXP\n  Unexpected invocation: " + this.ctr.name() + "." + meth + " " + args);
	}
	matched.invoked++;
	if (matched.invoked > matched.allowed) {
		throw new Error("EXP\n  " + this.ctr.name() + "." + meth + " " + args + " already invoked (allowed=" + matched.allowed +"; actual=" + matched.invoked +")");
	}
	_cxt.log("Have invocation of", meth, "with", args);
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

const MockAgent = function(agent) {
	this.agent = agent;
};

MockAgent.prototype.sendTo = function(_cxt, contract, msg, args) {
	const ctr = this.agent._contracts.contractFor(_cxt, contract);
	const inv = Array.from(args);
	inv.splice(0, 0, _cxt);
	return ctr[msg].apply(ctr, inv);
};



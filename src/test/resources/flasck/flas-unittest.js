// TODO: this should be in something different from runner, I think
// Probably we should have a "runner" module, and then something that imports all that and binds it onto Window
// Or use the same "export" technique we do elsewhere ...
// But console.log is JUST for the Java case
/*
window.console = {};
window.console.log = function() {
	var ret = '';
	var sep = '';
	for (var i=0;i<arguments.length;i++) {
		ret += sep + arguments[i];
		sep = ' ';
	}
	callJava.log(ret);
};
*/

window.runner = {};
window.runner.assertSameValue = function(_cxt, e, a) {
	e = _cxt.full(e);
	a = _cxt.full(a);
	if (!_cxt.compare(e, a)) {
		throw new Error("NSV" + "\n  expected: " + e + "\n  actual:   " + a);
	}
}
window.runner.invoke = function(_cxt, inv) {
	inv = _cxt.full(inv);
	handleMessages(_cxt, inv);
}
handleMessages = function(_cxt, msg) {
	if (msg instanceof Array) {
		for (var i=0;i<msg.length;i++) {
			handleMessages(_cxt, msg[i]);
		}
	} else if (msg) {
		var ret = msg.dispatch(_cxt);
		if (ret)
			handleMessages(_cxt, ret);
	}
}
window.runner.newContext = function() {
	return new FLContext(this);
}

const MockContract = function(ctr) {
	this.ctr = ctr;
};

MockContract.prototype.areYouA = function(ty) {
	return this.ctr.name() == ty;
}

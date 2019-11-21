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
	console.log(inv);
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

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
window.runner = {};
window.runner.assertSameValue = function(_cxt, e, a) {
	e = _cxt.full(e);
	a = _cxt.full(a);
	if (a != e) { // should be deep equal
		throw new Error("NSV" + "\n  expected: " + e + "\n  actual:   " + a);
	}
}
window.runner.newContext = function() {
	return new FLContext(this);
}


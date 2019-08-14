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
window.runner.assertSameValue = function(e, a) {
//	console.log(e);
//	console.log(a);
	if (a != e) { // should be deep equal
		throw new Error("NSV" + "\n  expected: " + e + "\n  actual:   " + a);
	}
}

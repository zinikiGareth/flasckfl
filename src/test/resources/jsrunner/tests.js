test = {};
test.flas = {};
test.flas.testrunner = {};
test.flas.testrunner.samples = {};

f = function() {
	return 42;
}
f.nfargs = function() { return 0 };

test.flas.testrunner.samples._ut12 = function(runner) {
	const _cxt = runner.newContext();
	runner.assertSameValue(_cxt, 42, 42);
}
	
test.flas.testrunner.samples._ut18 = function(runner) {
	const _cxt = runner.newContext();
	runner.assertSameValue(_cxt, 42, 84);
}

test.flas.testrunner.samples._ut25 = function(runner) {
	const _cxt = runner.newContext();
	const v1 = _cxt.closure(f);
	runner.assertSameValue(_cxt, v1, 42);
}

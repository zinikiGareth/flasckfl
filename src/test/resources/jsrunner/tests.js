test = {};
test.flas = {};
test.flas.testrunner = {};
test.flas.testrunner.samples = {};

f = function() {
	return 42;
}

test.flas.testrunner.samples._ut12 = function(runner) {
	runner.assertSameValue(42, 42);
}
	
test.flas.testrunner.samples._ut18 = function(runner) {
	runner.assertSameValue(42, 84);
}

test.flas.testrunner.samples._ut25 = function(runner) {
	const v1 = FLEval.closure(f);
	runner.assertSameValue(v1, 42);
}

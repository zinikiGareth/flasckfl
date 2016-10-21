if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden.f = function(v0) {
  "use strict";
  var v1 = FLEval.closure(FLEval.curry, test.golden.f_0.g, 2, v0);
  return FLEval.closure(v1, 2);
}

test.golden.f_0.g = function(s0, v0) {
  "use strict";
  return FLEval.closure(FLEval.mul, s0, v0);
}

test.golden;

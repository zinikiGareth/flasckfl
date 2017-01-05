if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden.f = function(v2) {
  "use strict";
  var v3 = FLEval.closure(FLEval.curry, test.golden.f_0.g, 2, v2);
  return FLEval.closure(v3, 2);
}

test.golden.f_0.g = function(s0, v0) {
  "use strict";
  return FLEval.closure(FLEval.mul, s0, v0);
}

test.golden;

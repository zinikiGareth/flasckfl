if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden.dbl = function() {
  "use strict";
  return FLEval.closure(FLEval.mul, test.golden.x, 2);
}

test.golden.x = function() {
  "use strict";
  return 32;
}

test.golden;

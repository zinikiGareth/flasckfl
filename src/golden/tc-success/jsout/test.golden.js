if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden.bert = function() {
  "use strict";
  return FLEval.closure(FLEval.tuple, 2, 'hello');
}

test.golden.fred = function(v0) {
  "use strict";
  return 32;
}

test.golden.t1 = function() {
  "use strict";
  return FLEval.closure(map, test.golden.fred, Nil);
}

test.golden.t2 = function() {
  "use strict";
  var v0 = FLEval.closure(Cons, 3, Nil);
  return FLEval.closure(map, test.golden.fred, v0);
}

test.golden;

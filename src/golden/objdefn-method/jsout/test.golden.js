if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._WithMethod = function() {
  "use strict";
}

test.golden.WithMethod.prototype.highTime = function() {
  "use strict";
  var v0 = FLEval.closure(Assign, this, 'x', 420);
  return FLEval.closure(Cons, v0, Nil);
}

test.golden;

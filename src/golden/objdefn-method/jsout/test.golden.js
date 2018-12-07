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

test.golden.WithMethod.prototype.highTime = function(v0) {
  "use strict";
  var v1 = FLEval.closure(Assign, this, 'x', 420);
  return FLEval.closure(Cons, v1, Nil);
}

test.golden;

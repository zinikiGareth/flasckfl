if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden.filter = function(v0, v1) {
  "use strict";
  v1 = FLEval.head(v1);
  if (v1 instanceof FLError) {
    return v1;
  }
  if (FLEval.isA(v1, 'Cons')) {
    var v2 = v1.head;
    var v3 = v1.tail;
    var v4 = FLEval.closure(v0, v2);
    if (v4) {
      var v5 = FLEval.closure(test.golden.filter, v0, v3);
      return FLEval.closure(Cons, v2, v5);
    }
    return FLEval.closure(test.golden.filter, v0, v3);
  }
  if (FLEval.isA(v1, 'Nil')) {
    return Nil;
  }
  return FLEval.error("test.golden.filter: case not handled");
}

test.golden;

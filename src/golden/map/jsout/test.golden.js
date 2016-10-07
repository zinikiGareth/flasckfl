if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden.map1 = function(v0, v1) {
  "use strict";
  v1 = FLEval.head(v1);
  if (v1 instanceof FLError) {
    return v1;
  }
  if (FLEval.isA(v1, 'Cons')) {
    var v2 = v1.head;
    var v3 = v1.tail;
    var v4 = FLEval.closure(v0, v2);
    var v5 = FLEval.closure(test.golden.map1, v0, v3);
    return FLEval.closure(Cons, v4, v5);
  }
  if (FLEval.isA(v1, 'Nil')) {
    return Nil;
  }
  return FLEval.error("test.golden.map1: case not handled");
}

test.golden;

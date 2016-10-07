if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden.take = function(v0, v1) {
  "use strict";
  v1 = FLEval.head(v1);
  if (v1 instanceof FLError) {
    return v1;
  }
  if (FLEval.isA(v1, 'Cons')) {
    var v2 = v1.head;
    var v3 = v1.tail;
    v0 = FLEval.head(v0);
    if (v0 instanceof FLError) {
      return v0;
    }
    if (FLEval.isInteger(v0)) {
      if (v0 === 0) {
        return Nil;
      }
    }
    var v4 = FLEval.closure(FLEval.minus, v0, 1);
    var v5 = FLEval.closure(test.golden.take, v4, v3);
    return FLEval.closure(Cons, v2, v5);
  }
  if (FLEval.isA(v1, 'Nil')) {
    return Nil;
  }
  return FLEval.error("test.golden.take: case not handled");
}

test.golden;

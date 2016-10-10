if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden.fold = function(v0, v1, v2) {
  "use strict";
  v2 = FLEval.head(v2);
  if (v2 instanceof FLError) {
    return v2;
  }
  if (FLEval.isA(v2, 'Cons')) {
    var v3 = v2.head;
    var v4 = v2.tail;
    var v5 = FLEval.closure(v0, v1, v3);
    return FLEval.closure(test.golden.fold, v0, v5, v4);
  }
  if (FLEval.isA(v2, 'Nil')) {
    return v1;
  }
  return FLEval.error("test.golden.fold: case not handled");
}

test.golden;

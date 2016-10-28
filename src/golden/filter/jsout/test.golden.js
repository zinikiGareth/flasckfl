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
    var v4 = FLEval.closure(FLEval.curry, test.golden.filter_1.g, 4, v2, v0, v3);
    var v5 = FLEval.closure(v0, v2);
    return FLEval.closure(v4, v5);
  }
  if (FLEval.isA(v1, 'Nil')) {
    return Nil;
  }
  return FLEval.error("test.golden.filter: case not handled");
}

test.golden.filter_1.g = function(s0, s1, s2, v7) {
  "use strict";
  v7 = FLEval.head(v7);
  if (v7 instanceof FLError) {
    return v7;
  }
  if (typeof v7 === 'boolean') {
    if (v7 === false) {
      return FLEval.closure(test.golden.filter, s1, s2);
    }
    if (v7 === true) {
      var v9 = FLEval.closure(test.golden.filter, s1, s2);
      return FLEval.closure(Cons, s0, v9);
    }
  }
  return FLEval.error("test.golden.filter_1.g: case not handled");
}

test.golden;

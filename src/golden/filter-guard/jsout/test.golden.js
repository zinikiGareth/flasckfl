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

test.golden.filter_1.g = function(s0, s1, s2, v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (typeof v0 === 'boolean') {
    if (v0 === false) {
      return FLEval.closure(test.golden.filter, s1, s2);
    }
    if (v0 === true) {
      var v2 = FLEval.closure(test.golden.filter, s1, s2);
      return FLEval.closure(Cons, s0, v2);
    }
  }
  return FLEval.error("test.golden.filter_1.g: case not handled");
}

test.golden;

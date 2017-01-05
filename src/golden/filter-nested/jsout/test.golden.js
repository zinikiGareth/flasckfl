if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden.filter = function(v4, v5) {
  "use strict";
  v5 = FLEval.head(v5);
  if (v5 instanceof FLError) {
    return v5;
  }
  if (FLEval.isA(v5, 'Cons')) {
    var v6 = v5.head;
    var v7 = v5.tail;
    var v8 = FLEval.closure(FLEval.curry, test.golden.filter_1.g, 4, v6, v4, v7);
    var v9 = FLEval.closure(v4, v6);
    return FLEval.closure(v8, v9);
  }
  if (FLEval.isA(v5, 'Nil')) {
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
      var v1 = FLEval.closure(test.golden.filter, s1, s2);
      return FLEval.closure(Cons, s0, v1);
    }
  }
  return FLEval.error("test.golden.filter_1.g: case not handled");
}

test.golden;

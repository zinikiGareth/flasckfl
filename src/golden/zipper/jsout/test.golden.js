if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden.zip = function(v0, v1) {
  "use strict";
  v1 = FLEval.head(v1);
  if (v1 instanceof FLError) {
    return v1;
  }
  if (FLEval.isA(v1, 'Cons')) {
    var v4 = v1.head;
    var v5 = v1.tail;
    v0 = FLEval.head(v0);
    if (v0 instanceof FLError) {
      return v0;
    }
    if (FLEval.isA(v0, 'Cons')) {
      var v2 = v0.head;
      var v3 = v0.tail;
      var v6 = FLEval.closure(FLEval.tuple, v2, v4);
      var v7 = FLEval.closure(test.golden.zip, v3, v5);
      return FLEval.closure(Cons, v6, v7);
    }
    return Nil;
  }
  return Nil;
}

test.golden;

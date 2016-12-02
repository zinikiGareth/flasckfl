if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden.f = function(v0, v1) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'test.golden.DoIt')) {
    var v2 = FLEval.closure(test.golden.f_0.m, v0, v1);
    var v3 = FLEval.closure(Cons, v2, Nil);
    return FLEval.closure(MessageWrapper, 0, v3);
  }
  return FLEval.error("test.golden.f: case not handled");
}

test.golden.f_0.m = function(s0, s1) {
  "use strict";
  var v5 = FLEval.closure(Cons, s1, Nil);
  var v6 = FLEval.closure(Send, s0, 'calc', v5);
  return FLEval.closure(Cons, v6, Nil);
}

test.golden;

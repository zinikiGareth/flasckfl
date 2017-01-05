if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden.f = function(v3, v4) {
  "use strict";
  v3 = FLEval.head(v3);
  if (v3 instanceof FLError) {
    return v3;
  }
  if (FLEval.isA(v3, 'test.golden.DoIt')) {
    var v5 = FLEval.closure(test.golden.f_0.m, v3, v4);
    var v6 = FLEval.closure(Cons, v5, Nil);
    return FLEval.closure(MessageWrapper, 0, v6);
  }
  return FLEval.error("test.golden.f: case not handled");
}

test.golden.f_0.m = function(s0, s1) {
  "use strict";
  var v0 = FLEval.closure(Cons, s1, Nil);
  var v1 = FLEval.closure(Send, s0, 'calc', v0);
  return FLEval.closure(Cons, v1, Nil);
}

test.golden;

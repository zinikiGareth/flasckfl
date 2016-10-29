if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

if (typeof test.golden.f_0 === 'undefined') {
  test.golden.f_0 = function() {
  }
}

if (typeof test.golden.f_0.HandleReply === 'undefined') {
  test.golden.f_0.HandleReply = function() {
  }
}

test.golden.f_0._HandleReply = function(v0, v1, v2) {
  "use strict";
  this._ctor = 'test.golden.f_0.HandleReply';
  this._special = 'handler';
  this._contract = 'test.golden.Reply';
  this.server = v0;
  this.var = v1;
  this.v = v2;
}

test.golden.f_0.HandleReply = function(v0, v1, v2) {
  "use strict";
  return new test.golden.f_0._HandleReply(v0, v1, v2);
}

test.golden.cna = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'test.golden.NoArg')) {
    var v1 = FLEval.closure(Cons, 42, Nil);
    var v2 = FLEval.closure(Send, v0, 'get', v1);
    return FLEval.closure(Cons, v2, Nil);
  }
  return FLEval.error("test.golden.cna: case not handled");
}

test.golden.f = function(v0, v1) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'test.golden.Server')) {
    var v4 = FLEval.closure(test.golden.f_0.var);
    var v2 = FLEval.closure(FLEval.curry, test.golden.f_0.HandleReply, 3, v0, v4);
    var v3 = FLEval.closure(test.golden.f_0.q, v2, v0, v4);
    var v5 = FLEval.closure(Cons, v3, Nil);
    return FLEval.closure(MessageWrapper, v4, v5);
  }
  return FLEval.error("test.golden.f: case not handled");
}

test.golden.f_0._HandleReply.prototype.reply = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isInteger(v0)) {
    var v1 = FLEval.closure(FLEval.plus, this.var, this.v);
    var v2 = FLEval.closure(Cons, v1, Nil);
    var v3 = FLEval.closure(Send, this.server, 'put', v2);
    return FLEval.closure(Cons, v3, Nil);
  }
  return FLEval.error("test.golden.f_0.HandleReply.reply: case not handled");
}

test.golden.f_0.q = function(s0, s1, s2) {
  "use strict";
  var v2 = FLEval.closure(s0, s2);
  var v3 = FLEval.closure(Cons, v2, Nil);
  var v4 = FLEval.closure(Cons, s2, v3);
  var v5 = FLEval.closure(Send, s1, 'get', v4);
  return FLEval.closure(Cons, v5, Nil);
}

test.golden.f_0.var = function() {
  "use strict";
  return 32;
}

test.golden;

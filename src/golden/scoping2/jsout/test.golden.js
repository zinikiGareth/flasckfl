if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

if (typeof test.golden.scoped_0 === 'undefined') {
  test.golden.scoped_0 = function() {
  }
}

if (typeof test.golden.scoped_0.MyHc === 'undefined') {
  test.golden.scoped_0.MyHc = function() {
  }
}

test.golden.scoped_0._MyHc = function(v0) {
  "use strict";
  this._ctor = 'test.golden.scoped_0.MyHc';
  this._special = 'handler';
  this._contract = 'test.golden.Hc';
  this.requestObj = v0;
}

test.golden.scoped_0.MyHc = function(v0) {
  "use strict";
  return new test.golden.scoped_0._MyHc(v0);
}

test.golden.scoped = function(v4) {
  "use strict";
  v4 = FLEval.head(v4);
  if (v4 instanceof FLError) {
    return v4;
  }
  if (FLEval.isA(v4, 'test.golden.UpC')) {
    var v5 = FLEval.closure(test.golden.scoped_0.MyHc, );
    var v6 = FLEval.closure(test.golden.scoped_0.request, v5, v4);
    var v7 = FLEval.closure(Cons, v6, Nil);
    return FLEval.closure(MessageWrapper, 32, v7);
  }
  return FLEval.error("test.golden.scoped: case not handled");
}

test.golden.scoped_0._MyHc.prototype.reply = function() {
  "use strict";
  var v0 = FLEval.closure(this.requestObj);
  return FLEval.closure(Cons, v0, Nil);
}

test.golden.scoped_0.request = function(s0, s1) {
  "use strict";
  var v0 = FLEval.closure(s0);
  var v1 = FLEval.closure(Cons, v0, Nil);
  var v2 = FLEval.closure(Send, s1, 'call', v1);
  return FLEval.closure(Cons, v2, Nil);
}

test.golden.scoped_0.requestObj = function() {
  "use strict";
  return Nil;
}

test.golden;

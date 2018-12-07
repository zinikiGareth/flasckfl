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

test.golden.scoped = function(v3) {
  "use strict";
  v3 = FLEval.head(v3);
  if (v3 instanceof FLError) {
    return v3;
  }
  if (FLEval.isA(v3, 'test.golden.UpC')) {
    var v4 = FLEval.closure(test.golden.scoped_0.MyHc, );
    var v5 = FLEval.closure(test.golden.scoped_0.request, v4, v3);
    var v6 = FLEval.closure(Cons, v5, Nil);
    return FLEval.closure(MessageWrapper, 32, v6);
  }
  return FLEval.error("test.golden.scoped: case not handled");
}

test.golden.scoped_0._MyHc.prototype.reply = function(v0) {
  "use strict";
  var v1 = FLEval.closure(this.requestObj);
  return FLEval.closure(Cons, v1, Nil);
}

test.golden.scoped_0.request = function(s0, s1) {
  "use strict";
  var v0 = FLEval.closure(s0);
  var v1 = FLEval.closure(Send, s1, 'call', Nil, v0);
  return FLEval.closure(Cons, v1, Nil);
}

test.golden.scoped_0.requestObj = function() {
  "use strict";
  return Nil;
}

test.golden;

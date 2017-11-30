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

if (typeof test.golden.f_0.KVHImpl === 'undefined') {
  test.golden.f_0.KVHImpl = function() {
  }
}

test.golden.f_0._KVHImpl = function(v0) {
  "use strict";
  this._ctor = 'test.golden.f_0.KVHImpl';
  this._special = 'handler';
  this._contract = 'test.golden.KVH';
  this.s = v0;
}

test.golden.f_0.KVHImpl = function(v0) {
  "use strict";
  return new test.golden.f_0._KVHImpl(v0);
}

test.golden.f = function() {
  "use strict";
  return Nil;
}

test.golden.f_0._KVHImpl.prototype.update = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'Crokeys')) {
    var v1 = FLEval.closure(Cons, v0, Nil);
    var v2 = FLEval.closure(Send, this.s, 'mergeAppend', v1);
    return FLEval.closure(Cons, v2, Nil);
  }
  return FLEval.error("test.golden.f_0.KVHImpl.update: case not handled");
}

test.golden.f_0.s = function() {
  "use strict";
  var v0 = FLEval.closure(FLEval.octor, Croset, 'from');
  return FLEval.closure(v0, Nil);
}

test.golden;

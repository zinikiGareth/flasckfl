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

if (typeof test.golden.f_0.ItemHandler === 'undefined') {
  test.golden.f_0.ItemHandler = function() {
  }
}

test.golden.f_0._ItemHandler = function(v0, v1) {
  "use strict";
  this._ctor = 'test.golden.f_0.ItemHandler';
  this._special = 'handler';
  this._contract = 'test.golden.KVUpdate';
  this.set = v0;
  this.ck = v1;
}

test.golden.f_0.ItemHandler = function(v0, v1) {
  "use strict";
  return new test.golden.f_0._ItemHandler(v0, v1);
}

test.golden.f = function() {
  "use strict";
  return FLEval.closure(test.golden.f_0.set);
}

test.golden.f_0._ItemHandler.prototype.update = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (v0) {
    var v1 = FLEval.closure(Cons, v0, Nil);
    var v2 = FLEval.closure(Cons, this.ck, v1);
    var v3 = FLEval.closure(Send, this.set, 'insert', v2);
    return FLEval.closure(Cons, v3, Nil);
  }
  return FLEval.error("test.golden.f_0.ItemHandler.update: case not handled");
}

test.golden.f_0.set = function() {
  "use strict";
  var v0 = FLEval.closure(FLEval.octor, Croset, 'from');
  return FLEval.closure(v0, Nil);
}

test.golden;

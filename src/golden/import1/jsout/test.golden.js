if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._Foo = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.Foo';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
  this._contracts['test.import.Fred'] = test.golden.Foo._C0.apply(this);
}

test.golden.Foo = function(v0) {
  "use strict";
  return new test.golden._Foo(v0);
}

test.golden.Foo.__C0 = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Foo._C0';
  this._card = v0;
  this._special = 'contract';
  this._contract = 'test.import.Fred';
}

test.golden.Foo._C0 = function() {
  "use strict";
  return new test.golden.Foo.__C0(this);
}

test.golden._Foo.prototype._render = function(doc, wrapper, parent) {
  "use strict";
}

test.golden.Foo.__C0.prototype.go = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isInteger(v0)) {
    var v1 = FLEval.closure(test.import.f, v0);
    var v2 = FLEval.closure(Assign, this._card, 'a', v1);
    return FLEval.closure(Cons, v2, Nil);
  }
  return FLEval.error("test.golden.Foo._C0.go: case not handled");
}

test.golden;

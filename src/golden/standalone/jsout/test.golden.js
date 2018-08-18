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
  this._contracts['test.golden.KeyValue'] = test.golden.Foo._C0.apply(this);
  this.kv = this._contracts['test.golden.KeyValue'];
  this._contracts['test.golden.Bar'] = test.golden.Foo._C1.apply(this);
}

test.golden._Foo.prototype._onReady = function(v0) {
  "use strict";
  var msgs = {curr: Nil};
  return msgs.curr;
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
  this._contract = 'test.golden.KeyValue';
}

test.golden.Foo._C0 = function() {
  "use strict";
  return new test.golden.Foo.__C0(this);
}

test.golden.Foo.__C1 = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Foo._C1';
  this._card = v0;
  this._special = 'contract';
  this._contract = 'test.golden.Bar';
}

test.golden.Foo._C1 = function() {
  "use strict";
  return new test.golden.Foo.__C1(this);
}

test.golden._Foo.prototype._render = function(doc, wrapper, parent) {
  "use strict";
}

test.golden.Foo.__C1.prototype.go = function() {
  "use strict";
  var v5 = FLEval.oclosure(this._card, FLEval.curry, test.golden.Foo.prototype.request, 1);
  var v0 = FLEval.closure(map, v5, Nil);
  var v4 = FLEval.oclosure(this._card, FLEval.curry, test.golden.Foo.prototype.request2, 1);
  var v1 = FLEval.closure(map, v4, Nil);
  var v2 = FLEval.closure(Cons, v1, Nil);
  return FLEval.closure(Cons, v0, v2);
}

test.golden.Foo.prototype.request = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'Crokey')) {
    var v1 = FLEval.closure(Cons, v0, Nil);
    return FLEval.closure(Send, this.kv, 'typed', v1);
  }
  return FLEval.error("test.golden.Foo.request: case not handled");
}

test.golden.Foo.prototype.request2 = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'Crokey')) {
    var v1 = FLEval.closure(Cons, v0, Nil);
    return FLEval.closure(Send, this.kv, 'mine', v1);
  }
  return FLEval.error("test.golden.Foo.request2: case not handled");
}

test.golden;
